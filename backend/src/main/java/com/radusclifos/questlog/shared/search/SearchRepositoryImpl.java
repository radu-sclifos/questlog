package com.radusclifos.questlog.shared.search;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class SearchRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements SearchRepository<T, ID> {

    private final CriteriaSpecificationBuilder<T> builder;

    public SearchRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.builder = new CriteriaSpecificationBuilder<>(entityManager.getMetamodel().entity(entityInformation.getJavaType()));
    }

    @Override
    public Page<T> searchWithCriteria(SearchCriteria criteria) {
        var specification = builder.build(criteria);
        return findAll(specification, PageRequest.of(criteria.getPageNumber(), criteria.getPageSize()));
    }

    @Override
    public void validateCriteria(SearchCriteria criteria) {
        builder.validateClientCriteria(criteria);
    }

}
