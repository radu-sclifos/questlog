package com.radusclifos.questlog.company.repository;

import com.radusclifos.questlog.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByIgdbId(Long igdbId);

    List<Company> findAllByIgdbIdIn(Collection<Long> igdbIds);

}
