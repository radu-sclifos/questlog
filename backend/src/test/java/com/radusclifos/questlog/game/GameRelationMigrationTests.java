package com.radusclifos.questlog.game;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRelationMigrationTests {
    @Test
    void baselineAcceptsCurrentRelationTypesAndRejectsLegacyValues() {
        var dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:relation-upgrade;MODE=PostgreSQL", "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__baseline.sql")).execute(dataSource);
        var jdbc = new JdbcTemplate(dataSource);
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
        dataSource.destroy();
    }
}
