package com.radusclifos.questlog.game;

import com.radusclifos.questlog.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PostgresTest
@SpringBootTest
class GameRelationMigrationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void baselineAcceptsCurrentRelationTypesAndRejectsLegacyValues() {
        jdbc.update("insert into games(id,name) values(101,'Original'),(102,'Related')");
        var types = List.of("DLC", "EXPANSION", "STANDALONE_EXPANSION", "REMAKE",
                "REMASTER", "PORT", "EXPANDED_GAME", "FORK");
        for (var type : types) {
            jdbc.update("insert into game_relations(source_game_id,target_game_id,relation_type) values(101,102,?)",
                    type);
            jdbc.update("insert into pending_game_relations(source_game_id,target_igdb_id,relation_type) values(101,999,?)",
                    type);
        }
        jdbc.update("insert into game_relations(source_game_id,target_game_id,relation_type) values(102,101,'PARENT_GAME')");

        assertEquals(types.stream().sorted().toList(), jdbc.queryForList(
                "select relation_type from game_relations where source_game_id=101 order by relation_type", String.class));
        assertEquals(types.stream().sorted().toList(), jdbc.queryForList(
                "select relation_type from pending_game_relations where target_igdb_id=999 order by relation_type", String.class));
        assertEquals("PARENT_GAME", jdbc.queryForObject(
                "select relation_type from game_relations where source_game_id=102", String.class));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into game_relations(source_game_id,target_game_id,relation_type) values(101,102,'HAS_DLC')"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into pending_game_relations(source_game_id,target_igdb_id,relation_type) values(101,999,'HAS_DLC')"));
    }

}
