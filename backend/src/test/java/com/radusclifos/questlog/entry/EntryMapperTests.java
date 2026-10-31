package com.radusclifos.questlog.entry;

import com.radusclifos.questlog.entry.dto.SaveEntryRequest;
import com.radusclifos.questlog.entry.mapper.EntryMapper;
import com.radusclifos.questlog.game.Game;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.junit.jupiter.api.Assertions.*;

class EntryMapperTests {
    private final EntryMapper mapper = Mappers.getMapper(EntryMapper.class);

    @Test void mapsCatalogDataWithoutCopyingItIntoEntry() {
        var game = new Game("Celeste", "co_example");
        var request = new SaveEntryRequest(1L, 9.0, "Notes", EntryStatus.COMPLETED, EntryOwnership.OWN, 12);
        var entry = mapper.toEntity(request, game);
        assertSame(game, entry.getGame());
        assertEquals("Celeste", mapper.toDto(entry).getGame().getName());
        var dto = mapper.toDto(entry);
        assertEquals("co_example", dto.getGame().getCoverImageId());
        assertEquals(9.0, dto.getScore());
        game.rename("Updated title");
        assertEquals("Updated title", mapper.toDto(entry).getGame().getName());
    }

    @Test void replacesPersonalDetailsWithoutModifyingTheCatalog() {
        var game = new Game("Celeste", null);
        var entry = new Entry(game);
        entry.setScore(9.0);
        mapper.updateEntity(new SaveEntryRequest(1L, null, null, null, null, null), entry);
        assertNull(entry.getScore());
        assertSame(game, entry.getGame());
        assertEquals("Celeste", game.getName());
        assertEquals(EntryStatus.UNPLAYED, entry.getStatus());
        assertThrows(NullPointerException.class, () -> new Entry(null));
        assertNull(mapper.toDto(null));
        assertNull(mapper.toEntity(null, game));
    }
}
