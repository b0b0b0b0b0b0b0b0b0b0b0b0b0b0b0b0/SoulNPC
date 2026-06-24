package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;
import bm.b0b0b0.SoulNPC.config.settings.SoulNpcSerializerConfig;

public final class NpcFileData extends YamlSerializable {

    @Comment(value = {
            @CommentValue(" Внутренний ID (имя файла npcs/<id>.yml); при create без id — 1, 2, 3…")
    })
    public String id = "example";

    @Comment(value = {
            @CommentValue(" Включён ли NPC")
    })
    public boolean enabled = true;

    @NewLine
    @Comment(value = {
            @CommentValue(" Локация (world, x, y, z, yaw, pitch)")
    })
    public String world = "world";
    public double x = 0.5D;
    public double y = 64.0D;
    public double z = 0.5D;
    public float yaw = 0F;
    public float pitch = 0F;

    @Comment(value = {
            @CommentValue(" Packet entity ID (0 = автогенерация)")
    })
    public int entityId = 0;

    @NewLine
    @Comment(value = {
            @CommentValue(" Внешний вид: модель, голограмма, экипировка")
    })
    public NpcAppearanceData appearance = new NpcAppearanceData();

    @NewLine
    @Comment(value = {
            @CommentValue(" Поза частей тела player-модели (градусы)"),
            @CommentValue(" Используется при animation.type: CUSTOM")
    })
    public NpcPoseData pose = NpcPoseData.defaultPlayerPose();

    @NewLine
    @Comment(value = {
            @CommentValue(" Анимации: приветствие, кивок, MOB_POSE и т.д.")
    })
    public NpcAnimationData animation = new NpcAnimationData();

    @NewLine
    @Comment(value = {
            @CommentValue(" Клики по NPC — см. комментарии внутри interaction:"),
            @CommentValue(" префиксы [message]/[player]/[console]/[op], permission, звуки")
    })
    public NpcInteractionData interaction = new NpcInteractionData();

    @Comment(value = {
            @CommentValue(" Смотреть на ближайшего игрока в радиусе")
    })
    public boolean lookAtPlayers = true;

    @Comment(value = {
            @CommentValue(" Радиус слежения за игроком (блоки)")
    })
    public int lookAtRange = 32;

    @NewLine
    @Comment(value = {
            @CommentValue(" Декоративные предметы у ног (Item entity, не подбираются)")
    })
    public NpcGroundItemEffectData groundItems = new NpcGroundItemEffectData();

    @NewLine
    @Comment(value = {
            @CommentValue(" Per-NPC дистанция packet-модели (0 = из config performance)"),
            @CommentValue(" hologramViewDistance: 0 = packetViewDistance или global hologram")
    })
    public int packetViewDistance = 0;

    public int hologramViewDistance = 0;

    @Comment(value = {
            @CommentValue(" Permission для видимости NPC (пусто = все)")
    })
    public String visibilityPermission = "";

    public NpcFileData() {
        super(SoulNpcSerializerConfig.INSTANCE);
    }

    public NpcFileData(String id) {
        this();
        this.id = id;
    }

    public void prepareForYamlSave() {
        appearance.normalizePresentation();
        appearance.ensureEquipmentSlots();
        appearance.ensureMobEquipment();
        if (pose == null) {
            pose = NpcPoseData.defaultPlayerPose();
        }
        if (groundItems == null) {
            groundItems = new NpcGroundItemEffectData();
        }
        groundItems.ensureItems();
        interaction.ensureActionsMigrated();
    }
}
