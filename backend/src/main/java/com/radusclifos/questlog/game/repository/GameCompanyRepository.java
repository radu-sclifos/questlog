package com.radusclifos.questlog.game.repository;

import com.radusclifos.questlog.game.GameCompany;
import com.radusclifos.questlog.game.dto.GameSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface GameCompanyRepository extends JpaRepository<GameCompany, Long> {

    @Query(value = """
            select coalesce(bool_or(developer), false) as developer,
                   coalesce(bool_or(publisher), false) as publisher,
                   coalesce(bool_or(porting), false) as porting,
                   coalesce(bool_or(supporting), false) as supporting
            from game_companies
            where company_id = :companyId
            """, nativeQuery = true)
    RoleAvailability findRoleAvailability(Long companyId);

    @Query("""
            select new com.radusclifos.questlog.game.dto.GameSummaryDto(g.id, g.igdbId, g.name, g.coverImageId)
            from GameCompany c join c.game g
            where c.company.id = :companyId and (
                (:role = 'developer' and c.developer = true)
                or (:role = 'publisher' and c.publisher = true)
                or (:role = 'porting' and c.porting = true)
                or (:role = 'supporting' and c.supporting = true)
            )
            order by lower(g.name), g.id
            """)
    Page<GameSummaryDto> findGameSummariesByCompanyAndRole(Long companyId, String role, Pageable pageable);

    @Modifying
    @Query("delete from GameCompany c where c.game.id in :gameIds")
    void deleteForGames(@Param("gameIds") Collection<Long> gameIds);

    interface RoleAvailability {
        boolean getDeveloper();
        boolean getPublisher();
        boolean getPorting();
        boolean getSupporting();
    }

}
