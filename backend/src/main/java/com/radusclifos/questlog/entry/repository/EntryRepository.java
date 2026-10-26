package com.radusclifos.questlog.entry.repository;

import com.radusclifos.questlog.entry.Entry;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EntryRepository extends JpaRepository<Entry, Long> {
}
