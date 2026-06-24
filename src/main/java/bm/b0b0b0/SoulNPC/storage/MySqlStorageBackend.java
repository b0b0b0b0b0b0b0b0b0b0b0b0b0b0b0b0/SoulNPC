package bm.b0b0b0.SoulNPC.storage;

import bm.b0b0b0.SoulNPC.config.settings.SoulNpcSettings;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.util.NpcIdValidator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class MySqlStorageBackend implements NpcStorageBackend {

    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private final ExecutorService executor;

    public MySqlStorageBackend(JavaPlugin plugin, SoulNpcSettings.MySqlStorage mysql, ExecutorService executor) {
        this.plugin = plugin;
        this.executor = executor;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + mysql.host + ":" + mysql.port + "/" + mysql.database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
        config.setUsername(mysql.username);
        config.setPassword(mysql.password == null ? "" : mysql.password);
        config.setMaximumPoolSize(Math.max(1, mysql.poolSize));
        config.setPoolName("SoulNPC-MySQL");
        config.addDataSourceProperty("cachePrepStmts", "true");
        this.dataSource = new HikariDataSource(config);
        ensureSchemaSync();
    }

    @Override
    public StorageType type() {
        return StorageType.MYSQL;
    }

    @Override
    public CompletableFuture<Map<String, NpcFileData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, NpcFileData> result = new LinkedHashMap<>();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT id, payload FROM soulnpc_npcs ORDER BY id");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String id = resultSet.getString("id");
                    String payload = resultSet.getString("payload");
                    try {
                        NpcFileData data = NpcPayloadCodec.decode(payload, id);
                        result.put(NpcIdValidator.canonicalKey(data.id), data);
                    } catch (Exception exception) {
                        plugin.getLogger().warning("Failed to decode NPC " + id + ": " + exception.getMessage());
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().warning("Failed to load NPCs from MySQL: " + exception.getMessage());
            }
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(String id, NpcFileData data) {
        return CompletableFuture.runAsync(() -> {
            String storedId = NpcIdValidator.normalize(id);
            if (data.id == null || data.id.isBlank()) {
                data.id = storedId;
            } else {
                data.id = NpcIdValidator.normalize(data.id);
            }
            String key = NpcIdValidator.canonicalKey(data.id);
            try {
                String payload = NpcPayloadCodec.encode(data);
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("""
                             INSERT INTO soulnpc_npcs(id, payload, updated_at)
                             VALUES (?, ?, ?)
                             ON DUPLICATE KEY UPDATE payload = VALUES(payload), updated_at = VALUES(updated_at)
                             """)) {
                    statement.setString(1, key);
                    statement.setString(2, payload);
                    statement.setLong(3, System.currentTimeMillis());
                    statement.executeUpdate();
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save NPC " + data.id + ": " + exception.getMessage(),
                        exception);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM soulnpc_npcs WHERE id = ?")) {
                statement.setString(1, NpcIdValidator.canonicalKey(id));
                statement.executeUpdate();
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
        dataSource.close();
    }

    private void ensureSchemaSync() {
        try (Connection connection = dataSource.getConnection()) {
            SchemaMigrator.migrate(connection);
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to migrate MySQL schema: " + exception.getMessage());
        }
    }
}
