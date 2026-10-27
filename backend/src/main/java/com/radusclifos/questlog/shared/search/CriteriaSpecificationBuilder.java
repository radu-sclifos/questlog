package com.radusclifos.questlog.shared.search;

import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

final class CriteriaSpecificationBuilder<T> {

    private final EntityType<T> entity;

    private record ResolvedFilter(String field, Class<?> type, FilterOperator operator, Object value) {
    }

    CriteriaSpecificationBuilder(EntityType<T> entity) {
        this.entity = entity;
    }

    void validateClientCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            throw invalid("Search criteria required");
        }
        if (criteria.getFilters().size() > 20) {
            throw invalid("At most 20 filters are allowed");
        }

        for (var filter : criteria.getFilters()) {
            if (filter == null || filter.getField() == null || filter.getField().contains("."))
                throw invalid("Client filters must reference a direct entity field");
        }
        for (var sort : criteria.getSorts()) {
            if (sort == null || sort.getField() == null || sort.getField().contains("."))
                throw invalid("Client sorts must reference a direct entity field");
        }

        build(criteria);
    }

    Specification<T> build(SearchCriteria criteria) {
        if (criteria == null || criteria.getPageNumber() < 0 || criteria.getPageSize() < 1 || criteria.getPageSize() > 100) {
            throw invalid("pageNumber must be >= 0 and pageSize between 5 and 100");
        }
        if (criteria.getFilters().size() > 20 || criteria.getSorts().size() > 5) {
            throw invalid("Too many filters or sorts");
        }

        // ## Resolving filters
        var resolvedFilters = new ArrayList<ResolvedFilter>();
        for (var filter : criteria.getFilters()) {
            if (filter == null) {
                throw invalid("Null filter");
            }

            var type = fieldType(entity, filter.getField());
            Object value;

            var operator = filter.getOperator();
            if (operator == null) {
                throw invalid("Missing filter operator");
            }
            if (operator == FilterOperator.CONTAINS && type != String.class) {
                throw invalid("CONTAINS requires a text field");
            }
            if ((operator == FilterOperator.GREATER_THAN || operator == FilterOperator.LESS_THAN)
                    && !(Number.class.isAssignableFrom(type) || Temporal.class.isAssignableFrom(type) || java.util.Date.class.isAssignableFrom(type))) {
                throw invalid("Comparison requires a numeric or date field");
            }

            if (operator == FilterOperator.IS_NULL) {
                if (filter.getValue() != null) {
                    throw invalid("IS_NULL takes no value");
                }
                value = null;
            } else if (operator == FilterOperator.IN) {
                var values = filter.getValueList();
                if (values == null || values.isEmpty() || values.size() > 100) {
                    throw invalid("IN requires valueList with 1 to 100 values");
                }
                value = values.stream().map(item -> convert(item, type)).toList();
            } else {
                value = convert(filter.getValue(), type);
            }

            resolvedFilters.add(new ResolvedFilter(filter.getField(), type, operator, value));
        }

        // ## Resolving sorts
        var sorts = new ArrayList<SortCriteria>();
        var seen = new HashSet<String>();
        for (var sort : criteria.getSorts()) {
            if (sort == null || sort.getDirection() == null || !seen.add(sort.getField())) {
                throw invalid("Invalid or duplicate sort");
            }

            var type = fieldType(entity, sort.getField());
            if (!Comparable.class.isAssignableFrom(type)) {
                throw invalid("Field cannot be sorted: " + sort.getField());
            }

            var customOrder = sort.getCustomOrder();
            if (customOrder != null) {
                if (!type.isEnum() || customOrder.isEmpty() || customOrder.size() > 100) {
                    throw invalid("customOrder requires an enum field and 1 to 100 values");
                }
                var values = new HashSet<Object>();
                for (var value : customOrder) {
                    if (value == null || !values.add(convert(value, type))) {
                        throw invalid("customOrder contains null or duplicate values");
                    }
                }
            }

            sorts.add(sort);
        }

        // Unique IDs are internal tie-breakers
        entity.getSingularAttributes().stream().filter(SingularAttribute::isId)
                .map(Attribute::getName).sorted().forEach(name -> {
                    if (seen.add(name)) {
                        sorts.add(new SortCriteria(name, Sort.Direction.ASC));
                    }
                });

        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            for (var filter : resolvedFilters) {
                predicates.add(predicate(filter, root, cb));
            }

            // Spring Data may execute two queries: a SELECT and a COUNT.
            // We don't need ORDER BY (sort) on the COUNT query.
            // Here a ResultType of Long identifies the COUNT query so we skip sorting.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var orders = new ArrayList<Order>();
                for (var sort : sorts) {
                    var path = path(root, sort.getField());
                    if (sort.isNullsLast()) {
                        // Keep null placement independent of the value sort's direction.
                        var nullRank = cb.<Integer>selectCase()
                                .when(cb.isNull(path), 1)
                                .otherwise(0);
                        orders.add(cb.asc(nullRank));
                    }
                    Expression<?> expression = path.getJavaType() == String.class ? cb.lower(path.as(String.class)) : path;
                    if (sort.getCustomOrder() != null) {
                        var rank = cb.<Integer>selectCase();
                        for (int i = 0; i < sort.getCustomOrder().size(); i++) {
                            var value = convert(sort.getCustomOrder().get(i), path.getJavaType());
                            rank.when(cb.equal(path, value), i);
                        }
                        expression = rank.otherwise(sort.getCustomOrder().size());
                    }
                    orders.add(sort.getDirection().isAscending() ? cb.asc(expression) : cb.desc(expression));
                }
                query.orderBy(orders);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Class<?> fieldType(ManagedType<?> entity, String name) {
        if (name == null || name.isBlank()) {
            throw invalid("Missing search field");
        }

        try {
            var parts = name.split("\\.", -1);
            ManagedType<?> current = entity;

            for (int i = 0; i < parts.length; i++) {
                var attribute = current.getAttribute(parts[i]);
                if (!(attribute instanceof SingularAttribute<?, ?> singular)) {
                    throw invalid("Collection paths are not supported");
                }
                if (i == parts.length - 1) {
                    if (attribute.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC) {
                        throw invalid("Search field must be a basic attribute");
                    }
                    return wrapper(attribute.getJavaType());
                }
                if (!(singular.getType() instanceof ManagedType<?> managed)) {
                    throw invalid("Invalid field path: " + name);
                }
                current = managed;
            }
        } catch (IllegalArgumentException exception) {
            throw invalid("Unknown search field: " + name);
        }
        throw invalid("Unknown search field: " + name);
    }

    private static Class<?> wrapper(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == boolean.class) return Boolean.class;
        return type;
    }

    private static Path<?> path(Path<?> root, String name) {
        for (String part : name.split("\\.")) {
            root = root.get(part);
        }
        return root;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object value, Class<?> type) {
        if (value == null) {
            throw invalid("Use IS_NULL to filter null values");
        }
        try {
            Object converted;

            if (type.isInstance(value)) {
                converted = value;
            } else if (type.isEnum() && value instanceof String text) {
                converted = Enum.valueOf((Class) type, text);
            } else if (Number.class.isAssignableFrom(type) && (value instanceof Number || value instanceof String)) {
                var number = new java.math.BigDecimal(value.toString());
                if (type == Long.class) converted = number.longValueExact();
                else if (type == Integer.class) converted = number.intValueExact();
                else if (type == Short.class) converted = number.shortValueExact();
                else if (type == Byte.class) converted = number.byteValueExact();
                else if (type == Double.class) converted = number.doubleValue();
                else if (type == Float.class) converted = number.floatValue();
                else if (type == java.math.BigDecimal.class) converted = number;
                else if (type == java.math.BigInteger.class) converted = number.toBigIntegerExact();
                else throw invalid("Unsupported numeric type");
            } else {
                throw invalid("Invalid value type; expected " + type.getSimpleName());
            }

            if (converted instanceof Double number && !Double.isFinite(number)) {
                throw invalid("Value must be finite");
            }
            if (converted instanceof Float number && !Float.isFinite(number)) {
                throw invalid("Value must be finite");
            }

            return converted instanceof String text ? text.toLowerCase() : converted;
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw invalid("Invalid value for " + type.getSimpleName());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate predicate(ResolvedFilter filter, Root<?> root, CriteriaBuilder cb) {
        var path = path(root, filter.field());

        Expression<?> expression = filter.type() == String.class ? cb.lower(path.as(String.class)) : path;

        return switch (filter.operator()) {
            case EQUAL -> cb.equal(expression, filter.value());
            case IS_NULL -> cb.isNull(expression);
            case IN -> expression.in((Collection<?>) filter.value());
            case CONTAINS -> cb.like(expression.as(String.class), "%" + ((String) filter.value())
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%", '!');
            case GREATER_THAN -> cb.greaterThan((Expression) expression, (Comparable) filter.value());
            case LESS_THAN -> cb.lessThan((Expression) expression, (Comparable) filter.value());
        };
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

}
