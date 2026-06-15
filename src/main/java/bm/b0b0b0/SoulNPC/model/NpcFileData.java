package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;
import bm.b0b0b0.SoulNPC.config.settings.SoulNpcSerializerConfig;

public final class NpcFileData extends YamlSerializable {

    @Comment(value = {
            @CommentValue(" Уникальный ID NPC")
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
            @CommentValue(" Клики: команды на ПКМ / ЛКМ / СКМ / Shift")
    })
    public NpcInteractionData interaction = new NpcInteractionData();

    @Comment(value = {
            @CommentValue(" Смотреть на ближайшего игрока в радиусе")
    })
    public boolean lookAtPlayers = false;

    @Comment(value = {
            @CommentValue(" Радиус слежения за игроком (блоки)")
    })
    public int lookAtRange = 32;

    @NewLine
    @Comment(value = {
            @CommentValue(" Декоративные предметы у ног (Item entity, не подбираются)")
    })
    public NpcGroundItemEffectData groundItems = new NpcGroundItemEffectData();

    public NpcFileData() {
        super(SoulNpcSerializerConfig.INSTANCE);
    }

    public NpcFileData(String id) {
        this();
        this.id = id;
    }

    /** Перед первым save — все секции yaml заполнены дефолтами. */
    public void prepareForYamlSave() {
        appearance.ensureEquipmentSlots();
        if (pose == null) {
            pose = NpcPoseData.defaultPlayerPose();
        }
        if (groundItems == null) {
            groundItems = new NpcGroundItemEffectData();
        }
        groundItems.ensureItems();
    }
}
