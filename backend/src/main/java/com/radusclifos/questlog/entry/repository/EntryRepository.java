package com.radusclifos.questlog.entry.repository;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.shared.search.SearchRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends SearchRepository<Entry, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "game")
    Optional<Entry> findByIdAndUserId(Long id, Long userId);

}
