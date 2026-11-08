package com.radusclifos.questlog.support;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Clears test data without dropping the schema or Flyway's migration history.
 */
final class PostgresDatabaseReset {

    private PostgresDatabaseReset() {
    }

    static void reset() throws SQLException {
        try (var connection = SharedPostgres.connection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                var tables = new ArrayList<String>();
                try (var result = statement.executeQuery("""
                        select format('%I.%I', schemaname, tablename)
                        from pg_tables
                        where schemaname = 'public' and tablename <> 'flyway_schema_history'
                        order by tablename
                        """)) {
                    while (result.next()) tables.add(result.getString(1));
                }
                if (!tables.isEmpty()) {
                    // List all tables together to respect foreign keys. No CASCADE:
                    // unexpected cross-schema references should fail explicitly.
                    statement.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY");
                }
                var sequences = new ArrayList<String>();
                try (var result = statement.executeQuery("""
                        select format('ALTER SEQUENCE %I.%I RESTART WITH %s', schemaname, sequencename, start_value)
                        from pg_sequences where schemaname = 'public'
                        order by sequencename
                        """)) {
                    while (result.next()) sequences.add(result.getString(1));
                }
                // Batch sequences are standalone, so TRUNCATE RESTART IDENTITY
                // alone does not reset them.
                for (var sql : sequences) statement.execute(sql);
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            }
        }
    }

}
