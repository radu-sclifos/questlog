package com.radusclifos.questlog.game;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameFranchiseMigrationTests {
    @Test
    void baselineUsesOnlyTheFranchiseJoinTable() {
        var dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:franchise-upgrade;MODE=PostgreSQL", "sa", "", true);
        try {
            new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__baseline.sql")).execute(dataSource);
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.update("insert into franchises(id,name) values(8,'Franchise')");
            jdbc.update("insert into games(id,name) values(101,'First'),(102,'Second')");
            jdbc.update("insert into game_franchises(game_id,franchise_id) values(101,8),(102,8)");
            assertEquals(List.of(101L, 102L), jdbc.queryForList(
                    "select game_id from game_franchises where franchise_id=8 order by game_id", Long.class));
            assertEquals(0, jdbc.queryForObject("""
                    select count(*) from information_schema.columns
                    where table_name='GAMES' and column_name='MAIN_FRANCHISE_ID'
                    """, Integer.class));
        } finally {
            dataSource.destroy();
        }
    }
}
