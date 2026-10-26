package com.radusclifos.questlog.entry.mapper;

import com.radusclifos.questlog.entry.Entry;
import com.radusclifos.questlog.entry.dto.EntryDto;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class EntryMapper {

    public abstract EntryDto toDto(Entry entry);

    public abstract List<EntryDto> toDtoList(List<Entry> entries);

    // Creates a new entry through the domain constructor; the database generates its ID.
    public Entry toEntity(SaveEntryRequest request) {
        if (request == null) {
            return null;
        }
        var entry = new Entry(request.getTitle());
        copyDetails(request, entry);
        return entry;
    }

    // Replaces editable fields on an existing entry, preserving its ID.
    public void updateEntity(SaveEntryRequest request, Entry entry) {
        requireNonNull(request, "request must not be null");
        requireNonNull(entry, "entry must not be null");

        entry.rename(request.getTitle());
        copyDetails(request, entry);
    }

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", ignore = true)
    protected abstract void copyDetails(SaveEntryRequest request, @MappingTarget Entry entry);

}
