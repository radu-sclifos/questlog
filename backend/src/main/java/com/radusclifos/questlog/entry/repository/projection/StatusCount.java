package com.radusclifos.questlog.entry.repository.projection;

import com.radusclifos.questlog.entry.EntryStatus;

public interface StatusCount {

    EntryStatus getStatus();

    long getAmount();

}
