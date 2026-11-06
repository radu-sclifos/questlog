package com.radusclifos.questlog.game;

import com.radusclifos.questlog.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresTest
@SpringBootTest
class GameFranchiseMigrationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void baselineUsesOnlyTheFranchiseJoinTable() {
        jdbc.update("insert into franchises(id,name) values(8,'Franchise')");
        jdbc.update("insert into games(id,name) values(101,'First'),(102,'Second')");
        jdbc.update("insert into game_franchises(game_id,franchise_id) values(101,8),(102,8)");
        assertEquals(List.of(101L, 102L), jdbc.queryForList(
                "select game_id from game_franchises where franchise_id=8 order by game_id", Long.class));
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema='public' and table_name='games' and column_name='main_franchise_id'
                """, Integer.class));
    }

}
