package bm.b0b0b0.SoulNPC.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigrator {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS soulnpc_npcs (
                id VARCHAR(64) PRIMARY KEY,
                payload TEXT NOT NULL,
                updated_at BIGINT NOT NULL
            )
            """;

    private SchemaMigrator() {
    }

    public static void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(DDL);
        }
    }
}
