package com.radusclifos.questlog.entry.service;

import com.radusclifos.questlog.entry.EntryStatus;
import com.radusclifos.questlog.shared.search.SortCriteria;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Default sorting rules for entry searches without explicit sorts.
 */
public final class EntrySearchDefaults {

    private EntrySearchDefaults() {
    }

    public static final List<EntryStatus> STATUS_ORDER = List.of(
            EntryStatus.PLAYING,
            EntryStatus.ON_HOLD,
            EntryStatus.COMPLETED,
            EntryStatus.BEATEN,
            EntryStatus.ENDLESS,
            EntryStatus.DROPPED,
            EntryStatus.UNPLAYED
    );

    public static SortCriteria statusSort() {
        return new SortCriteria("status", Sort.Direction.ASC, STATUS_ORDER.stream().map(Enum::name).toList());
    }

    public static SortCriteria statusSort(Sort.Direction direction) {
        return new SortCriteria("status", direction, STATUS_ORDER.stream().map(Enum::name).toList());
    }

    public static List<SortCriteria> sorts() {
        return List.of(statusSort(), new SortCriteria("game.name", Sort.Direction.ASC));
    }

}
