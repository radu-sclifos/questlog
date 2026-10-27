package com.radusclifos.questlog.entry.repository;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.shared.search.SearchRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends SearchRepository<Entry, Long> {

    Optional<Entry> findByIdAndUserId(Long id, Long userId);

}
