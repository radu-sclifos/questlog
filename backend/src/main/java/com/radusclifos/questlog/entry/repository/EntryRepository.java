package com.radusclifos.questlog.entry.repository;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.EntryStatus;
import com.radusclifos.questlog.entry.repository.projection.ProfileTotals;
import com.radusclifos.questlog.entry.repository.projection.StatusCount;
import com.radusclifos.questlog.shared.search.SearchRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends SearchRepository<Entry, Long> {

    @Query("""
            select count(e) as totalGames, count(e.score) as ratedGames,
                   avg(e.score) as averageRating, sum(e.playtime) as loggedPlaytime
            from Entry e where e.user.id = :userId
            """)
    ProfileTotals profileTotals(Long userId);

    @Query("""
            select e.status as status, count(e) as amount from Entry e
            where e.user.id = :userId group by e.status
            """)
    List<StatusCount> profileStatuses(Long userId);

    @EntityGraph(attributePaths = {"game", "platform"})
    List<Entry> findByUserIdAndStatusOrderByGameNameAscIdAsc(Long userId,
                                                             EntryStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "game")
    Optional<Entry> findByIdAndUserId(Long id, Long userId);

}
