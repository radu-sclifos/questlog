package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByIgdbId(Long igdbId);

}
