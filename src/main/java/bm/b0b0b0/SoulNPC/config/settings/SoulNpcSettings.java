package bm.b0b0b0.SoulNPC.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SoulNpcSettings extends YamlSerializable {

    @NewLine
    @Comment(value = {
            @CommentValue(" Основные настройки SoulNPC")
    })
    public General general = new General();

    @NewLine
    @Comment(value = {
            @CommentValue(" Права доступа (permission nodes)")
    })
    public Permissions permissions = new Permissions();

    @NewLine
    @Comment(value = {
            @CommentValue(" Оптимизация и производительность")
    })
    public Performance performance = new Performance();

    @NewLine
    @Comment(value = {
            @CommentValue(" Внешний вид и кастомные предметы")
    })
    public Appearance appearance = new Appearance();

    @NewLine
    @Comment(value = {
            @CommentValue(" Хранение NPC: yaml (по умолчанию), sqlite, mysql"),
            @CommentValue(" Смена type требует перезапуск сервера")
    })
    public Storage storage = new Storage();

    @NewLine
    @Comment(value = {
            @CommentValue(" Дефолты новых NPC (/soulnpc create)")
    })
    public NpcDefaults npcDefaults = new NpcDefaults();

    public SoulNpcSettings() {
        super(SoulNpcSerializerConfig.INSTANCE);
    }

    public static final class NpcDefaults {
        @Comment(value = {
                @CommentValue(" GREET: приседание/кивок при приближении игрока (player-NPC)")
        })
        public boolean greetAnimation = false;

        @Comment(value = {
                @CommentValue(" Смотреть на ближайшего игрока у новых player-NPC")
        })
        public boolean lookAtPlayers = true;
    }

    public static final class General {
        @Comment(value = {
                @CommentValue(" Язык по умолчанию: ru, en")
        })
        public String defaultLocale = "ru";

        @Comment(value = {
                @CommentValue(" Устарело: используйте storage.yaml.folder")
        })
        public String npcFolder = "npcs";

        @Comment(value = {
                @CommentValue(" Автосоздание примера NPC при первом запуске")
        })
        public boolean createExampleNpc = false;

        @Comment(value = {
                @CommentValue(" Логи packet-spawn в консоль (entity id, профиль, этапы showTo)")
        })
        public boolean debugPackets = false;
    }

    public static final class Permissions {
        public String admin = "soulnpc.admin";
        public String use = "soulnpc.use";
        public String bypassCooldown = "soulnpc.bypass.cooldown";
        public String create = "soulnpc.create";
        public String delete = "soulnpc.delete";
        public String edit = "soulnpc.edit";
    }

    public static final class Performance {
        @Comment(value = {
                @CommentValue(" Интервал глобального тика анимаций (тики). 0 = отключено")
        })
        public int animationTickInterval = 2;

        @Comment(value = {
                @CommentValue(" Максимум анимированных NPC на один тик")
        })
        public int animationBatchSize = 16;

        @Comment(value = {
                @CommentValue(" Кулдаун взаимодействия по умолчанию (мс)")
        })
        public long defaultInteractionCooldownMs = 500L;

        @Comment(value = {
                @CommentValue(" Дистанция показа packet-NPC и голограммы (блоки)")
        })
        public int packetViewDistance = 48;

        @Comment(value = {
                @CommentValue(" Дистанция голограммы (блоки); 0 = как packet-view-distance")
        })
        public int hologramViewDistance = 0;

        @Comment(value = {
                @CommentValue(" Интервал проверки видимости packet-NPC (тики)")
        })
        public int packetTickInterval = 10;

        @Comment(value = {
                @CommentValue(" Сколько spawn/hide операций за один тик")
        })
        public int packetSpawnBatchSize = 48;
    }

    public static final class Storage {
        @Comment(value = {
                @CommentValue(" yaml | sqlite | mysql")
        })
        public String type = "yaml";

        public YamlStorage yaml = new YamlStorage();
        public SqliteStorage sqlite = new SqliteStorage();
        public MySqlStorage mysql = new MySqlStorage();
    }

    public static final class YamlStorage {
        @Comment(value = {
                @CommentValue(" Папка YAML-файлов NPC относительно папки плагина")
        })
        public String folder = "npcs";
    }

    public static final class SqliteStorage {
        @Comment(value = {
                @CommentValue(" Путь к SQLite-файлу относительно папки плагина")
        })
        public String file = "storage/soulnpc.db";
    }

    public static final class MySqlStorage {
        public String host = "localhost";
        public int port = 3306;
        public String database = "soulnpc";
        public String username = "root";
        public String password = "";
        @Comment(value = {
                @CommentValue(" Размер пула HikariCP")
        })
        public int poolSize = 10;
    }

    public static final class Appearance {
        @Comment(value = {
                @CommentValue(" Тип: PLAYER, FOX, ARMOR_STAND, AUTO")
        })
        public String defaultDisplayType = "PLAYER";

        @Comment(value = {
                @CommentValue(" Разрешить кастом model data на предметах экипировки")
        })
        public boolean allowCustomModelData = true;

        @Comment(value = {
                @CommentValue(" Префикс для ItemsAdder (пусто = выключено)")
        })
        public String itemsAdderPrefix = "";

        @Comment(value = {
                @CommentValue(" Префикс для Nexo (пусто = выключено)")
        })
        public String nexoPrefix = "";
    }
}
