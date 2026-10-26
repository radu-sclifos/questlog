package com.radusclifos.questlog.entry.repository;

import com.radusclifos.questlog.entry.Entry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<Entry> findByIdAndUserId(Long id, Long userId);

}
