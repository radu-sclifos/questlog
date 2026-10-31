package com.radusclifos.questlog.entry.mapper;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.dto.EntryDto;
import com.radusclifos.questlog.entry.dto.EntryGameDto;
import com.radusclifos.questlog.entry.dto.EntryPage;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.game.Game;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class EntryMapper {

    public abstract EntryDto toDto(Entry entry);

    public abstract EntryGameDto toEntryGameDto(Game game);

    public abstract List<EntryDto> toDtoList(List<Entry> entries);

    public EntryPage toDtoPage(Page<Entry> page) {
        return EntryPage.from(page.map(this::toDto));
    }

    // Creates an entry linked to an existing catalog game
    public Entry toEntity(SaveEntryRequest request, Game game) {
        if (request == null) {
            return null;
        }
        var entry = new Entry(game);
        copyDetails(request, entry);
        return entry;
    }

    // Replaces personal details without changing the catalog
    public void updateEntity(SaveEntryRequest request, Entry entry) {
        requireNonNull(request, "request must not be null");
        requireNonNull(entry, "entry must not be null");

        copyDetails(request, entry);
    }

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "game", ignore = true)
    protected abstract void copyDetails(SaveEntryRequest request, @MappingTarget Entry entry);

}
