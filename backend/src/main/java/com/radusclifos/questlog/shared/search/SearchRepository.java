package com.radusclifos.questlog.shared.search;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public interface SearchRepository<T, ID> extends JpaRepository<T, ID> {

    Page<T> searchWithCriteria(SearchCriteria criteria);

    /**
     * Validate client criteria before adding server-owned filters.
     * Does not execute a query.
     */
    void validateCriteria(SearchCriteria criteria);

}
