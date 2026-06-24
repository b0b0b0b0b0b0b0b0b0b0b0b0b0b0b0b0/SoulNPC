package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.model.NpcFileData;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class SqliteStorageBackend implements NpcStorageBackend {

    private final JavaPlugin plugin;
    private final Path databaseFile;
    private final ExecutorService executor;
    private final String jdbcUrl;

    public SqliteStorageBackend(JavaPlugin plugin, Path databaseFile, ExecutorService executor) {
        this.plugin = plugin;
        this.databaseFile = databaseFile;
        this.executor = executor;
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
    }

    @Override
    public StorageType type() {
        return StorageType.SQLITE;
    }

    @Override
    public CompletableFuture<Map<String, NpcFileData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, NpcFileData> result = new LinkedHashMap<>();
            try {
                ensureSchema();
                try (Connection connection = openConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT id, payload FROM soulnpc_npcs ORDER BY id");
                     ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String id = resultSet.getString("id");
                        String payload = resultSet.getString("payload");
                        try {
                            NpcFileData data = NpcPayloadCodec.decode(payload, id);
                            result.put(normalizeId(data.id), data);
                        } catch (Exception exception) {
                            plugin.getLogger().warning("Failed to decode NPC " + id + ": " + exception.getMessage());
                        }
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to load NPCs from SQLite: " + exception.getMessage());
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(String id, NpcFileData data) {
        return CompletableFuture.runAsync(() -> {
            String normalized = normalizeId(id);
            data.id = normalized;
            try {
                String payload = NpcPayloadCodec.encode(data);
                ensureSchema();
                try (Connection connection = openConnection();
                     PreparedStatement statement = connection.prepareStatement("""
                             INSERT INTO soulnpc_npcs(id, payload, updated_at)
                             VALUES (?, ?, ?)
                             ON CONFLICT(id) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at
                             """)) {
                    statement.setString(1, normalized);
                    statement.setString(2, payload);
                    statement.setLong(3, System.currentTimeMillis());
                    statement.executeUpdate();
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save NPC " + normalized + ": " + exception.getMessage(),
                        exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureSchema();
                try (Connection connection = openConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "DELETE FROM soulnpc_npcs WHERE id = ?")) {
                    statement.setString(1, normalizeId(id));
                    statement.executeUpdate();
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to delete NPC " + id + ": " + exception.getMessage(),
                        exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<String> nextAutoId(Map<String, NpcFileData> currentCache) {
        return CompletableFuture.supplyAsync(() -> {
            int candidate = 1;
            while (currentCache.containsKey(String.valueOf(candidate))) {
                candidate++;
            }
            return String.valueOf(candidate);
        }, executor);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    public Path databaseFile() {
        return databaseFile;
    }

    private void ensureSchema() throws SQLException {
        try {
            Files.createDirectories(databaseFile.getParent());
        } catch (Exception exception) {
            throw new SQLException("Failed to create SQLite directory: " + exception.getMessage(), exception);
        }
        try (Connection connection = openConnection()) {
            SchemaMigrator.migrate(connection);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private static String normalizeId(String id) {
        return id.toLowerCase();
    }
}
