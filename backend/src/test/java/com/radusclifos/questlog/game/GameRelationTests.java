package com.radusclifos.questlog.game;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GameRelationTests {
    @Test
    void rejectsSelfReferencesIncludingDifferentInstancesOfTheSameRow() {
        var source = new Game("Original", null);
        assertThrows(IllegalArgumentException.class,
                () -> new GameRelation(source, source, GameRelationType.REMAKE));
        var anotherInstance = new Game("Original", null);
        ReflectionTestUtils.setField(source, "id", 42L);
        ReflectionTestUtils.setField(anotherInstance, "id", 42L);
        assertThrows(IllegalArgumentException.class,
                () -> new GameRelation(source, anotherInstance, GameRelationType.REMAKE));
    }
}
