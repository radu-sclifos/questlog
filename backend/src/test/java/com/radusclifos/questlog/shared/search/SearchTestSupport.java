package com.radusclifos.questlog.shared.search;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.game.Game;
import com.radusclifos.questlog.user.User;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.*;
import java.lang.reflect.Modifier;
import java.util.*;
import static org.mockito.Mockito.*;

/** JPA metadata fixture for isolated controller tests; database tests use Hibernate's real metamodel. */
public final class SearchTestSupport {
    private SearchTestSupport() {}

    public static void configure(com.radusclifos.questlog.entry.repository.EntryRepository repository) {
        var builder = builder();
        doAnswer(invocation -> {
            builder.validateClientCriteria(invocation.getArgument(0));
            return null;
        }).when(repository).validateCriteria(any(SearchCriteria.class));
        when(repository.searchWithCriteria(any(SearchCriteria.class))).thenAnswer(invocation -> {
            SearchCriteria criteria = invocation.getArgument(0);
            builder.build(criteria);
            return new org.springframework.data.domain.PageImpl<com.radusclifos.questlog.entry.Entry>(
                java.util.List.of(), org.springframework.data.domain.PageRequest.of(criteria.getPageNumber(), criteria.getPageSize()), 0);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static CriteriaSpecificationBuilder<com.radusclifos.questlog.entry.Entry> builder() {
        var em = mock(EntityManager.class);
        var model = mock(Metamodel.class);
        when(em.getMetamodel()).thenReturn(model);
        Map<Class<?>, EntityType<?>> entities = new HashMap<>();
        for (var type : List.of(Entry.class, Game.class, User.class)) {
            var entity = mock(EntityType.class);
            doReturn(type).when(entity).getJavaType();
            entities.put(type, entity);
        }
        for (var item : entities.entrySet()) {
            var attributes = new HashMap<String, SingularAttribute>();
            for (var field : item.getKey().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                var attribute = mock(SingularAttribute.class);
                when(attribute.getName()).thenReturn(field.getName());
                doReturn(field.getType()).when(attribute).getJavaType();
                when(attribute.isId()).thenReturn(field.isAnnotationPresent(Id.class));
                when(attribute.getPersistentAttributeType()).thenReturn(field.isAnnotationPresent(ManyToOne.class)
                    ? Attribute.PersistentAttributeType.MANY_TO_ONE : Attribute.PersistentAttributeType.BASIC);
                if (entities.containsKey(field.getType())) doReturn(entities.get(field.getType())).when(attribute).getType();
                attributes.put(field.getName(), attribute);
            }
            doReturn(new HashSet<>(attributes.values())).when(item.getValue()).getSingularAttributes();
            when(item.getValue().getAttribute(anyString())).thenAnswer(invocation -> {
                var attribute = attributes.get(invocation.getArgument(0));
                if (attribute == null) throw new IllegalArgumentException("Unknown attribute");
                return attribute;
            });
        }
        when(model.entity(any(Class.class))).thenAnswer(invocation -> entities.get(invocation.getArgument(0)));
        return new CriteriaSpecificationBuilder<>(model.entity(Entry.class));
    }
}
