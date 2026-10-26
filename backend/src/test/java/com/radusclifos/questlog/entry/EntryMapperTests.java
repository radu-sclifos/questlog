package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.dto.EntryDto;
import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EntryMapperTests {

    private final EntryMapper mapper = Mappers.getMapper(EntryMapper.class);

    @Test
    void createsEntryThroughDomainConstructorAndMapsDetails() {
        SaveEntryRequest request = new SaveEntryRequest( "  Celeste  ", 9.0, "Notes", EntryStatus.COMPLETED, EntryOwnership.OWN, null);

        Entry entry = mapper.toEntity(request);

        assertNull(entry.getId());
        assertEquals("Celeste", entry.getTitle());
        EntryDto result = mapper.toDto(entry);
        assertEquals(request.getScore(), result.getScore());
        assertEquals(request.getNotes(), result.getNotes());
        assertEquals(request.getStatus(), result.getStatus());
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toEntity(new SaveEntryRequest(" ", null, null, null, null, null)));
    }

    @Test
    void updatesPreserveIdentityAndReplaceNullableFields() {
        Entry entry = new Entry("Celeste");
        ReflectionTestUtils.setField(entry, "id", 1L);
        entry.setScore(9.0);
        entry.setNotes("Old notes");
        entry.setStatus(EntryStatus.COMPLETED);

        mapper.updateEntity(new SaveEntryRequest("  Hollow Knight  ", null, null, null, null, null), entry);

        EntryDto result = mapper.toDto(entry);
        assertEquals(1L, result.getId());
        assertEquals("Hollow Knight", result.getTitle());
        assertNull(result.getScore());
        assertNull(result.getNotes());
        assertNull(result.getStatus());
        assertThrows(IllegalArgumentException.class,
                () -> mapper.updateEntity(new SaveEntryRequest(" ", null, null, null, null, null), entry));
        assertEquals("Hollow Knight", entry.getTitle());
    }

    @Test
    void preservesNullHandling() {
        assertNull(mapper.toDto(null));
        assertNull(mapper.toEntity(null));
        assertThrows(NullPointerException.class, () -> mapper.updateEntity(null, new Entry("Celeste")));
        assertThrows(NullPointerException.class,
                () -> mapper.updateEntity(new SaveEntryRequest("Celeste", null, null, null, null, null), null));
    }
}
