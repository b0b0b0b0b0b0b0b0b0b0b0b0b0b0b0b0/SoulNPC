package bm.b0b0b0.SoulNPC.model;

import bm.b0b0b0.SoulNPC.mob.NpcEntityTypeResolver;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;

import java.util.ArrayList;
import java.util.List;

@Comment(value = {
        @CommentValue(" Внешний вид NPC: тип модели, голограмма, размер, экипировка")
})
public final class NpcAppearanceData {

    @Comment(value = {
            @CommentValue(" Тип: PLAYER или MOB (+ entity-type)")
    })
    public NpcDisplayType type = NpcDisplayType.PLAYER;

    @Comment(value = {
            @CommentValue(" Вид моба (fox, fox_snow, zombie, …); для PLAYER оставь пустым")
    })
    public String entityType = "";

    @Comment(value = {
            @CommentValue(" Поза сущности: STANDING, SITTING, SLEEPING"),
            @CommentValue(" PLAYER: + CROUCHING, SWIMMING, SPIN_ATTACK, FALL_FLYING; SITTING — через невидимую стойку"),
            @CommentValue(" MOB: FOX/PANDA — при animation MOB_POSE берётся из цикла mob-poses")
    })
    public NpcEntityPose entityPose = NpcEntityPose.STANDING;

    @Comment(value = {
            @CommentValue(" MOB: статичная поза модели (не цикл MOB_POSE)"),
            @CommentValue(" STANDING; ON_BACK (allay); HANGING (bat)")
    })
    public NpcMobDisplayPose mobDisplayPose = NpcMobDisplayPose.STANDING;

    /** @deprecated окрас лисы задаётся через entity-type: fox или fox_snow */
    @Deprecated
    public NpcFoxVariant foxVariant = NpcFoxVariant.RED;

    @NewLine
    @Comment(value = {
            @CommentValue(" Голограмма (TextDisplay) — строки над NPC, MiniMessage")
    })
    public String name = "<white>NPC</white>";

    @Comment(value = {
            @CommentValue(" Вторая строка голограммы")
    })
    public String description = "";

    @Comment(value = {
            @CommentValue(" Скин player NPC: ник, имя скина SkinRestorer (/skin set), URL"),
            @CommentValue(" Без SkinRestorer — только Mojang по нику")
    })
    public String profile = "";

    @Comment(value = {
            @CommentValue(" true — строки через TextDisplay; false — nametag над головой пакетного игрока")
    })
    public boolean useTextDisplay = true;

    @Comment(value = {
            @CommentValue(" Высота центра нижней строки голограммы от ног (блоки)")
    })
    public float hologramBaseOffset = 2.25F;

    @Comment(value = {
            @CommentValue(" Расстояние между строками голограммы (блоки)")
    })
    public float hologramLineSpacing = 0.28F;

    @Comment(value = {
            @CommentValue(" Примерная высота одной строки для стека (блоки)")
    })
    public float hologramLineHeight = 0.26F;

    @Comment(value = {
            @CommentValue(" Масштаб строки name в голограмме")
    })
    public float nameDisplayScale = 1.0F;

    @Comment(value = {
            @CommentValue(" Масштаб description и extra-lines")
    })
    public float descriptionDisplayScale = 0.85F;

    @Comment(value = {
            @CommentValue(" TextDisplay: сквозь блоки")
    })
    public boolean hologramSeeThrough = true;

    @Comment(value = {
            @CommentValue(" TextDisplay: тень текста")
    })
    public boolean hologramShadowed = true;

    @Comment(value = {
            @CommentValue(" TextDisplay: полупрозрачный фон за текстом")
    })
    public boolean hologramBackground = false;

    @Comment(value = {
            @CommentValue(" Доп. строки голограммы (линия 3, 4, …), MiniMessage")
    })
    public List<String> extraLines = new ArrayList<>();

    @Deprecated
    public float nameDisplayOffset = 2.25F;
    @Deprecated
    public float descriptionDisplayOffset = 2.53F;

    @NewLine
    @Comment(value = {
            @CommentValue(" Модель player NPC (для MOB — из профиля моба)")
    })
    public boolean glow = false;

    @Comment(value = {
            @CommentValue(" Размер модели (1.0 = обычный; 0.5 ≈ small)")
    })
    public float scale = 1.0F;

    /** @deprecated используй {@link #scale} */
    @Deprecated
    public boolean small = false;

    @Comment(value = {
            @CommentValue(" Невидимая модель (голограмма/хитбокс остаются)")
    })
    public boolean invisible = false;

    @Comment(value = {
            @CommentValue(" Показывать руки у player-модели")
    })
    public boolean arms = true;

    @Comment(value = {
            @CommentValue(" Отключить гравитацию у packet-сущности")
    })
    public boolean noGravity = true;

    @Comment(value = {
            @CommentValue(" Marker hitbox (только для armor_stand-типа)")
    })
    public boolean marker = false;

    @NewLine
    @Comment(value = {
            @CommentValue(" Экипировка player NPC (для MOB не используется)")
    })
    public NpcEquipmentSlotData helmet = new NpcEquipmentSlotData();

    public NpcEquipmentSlotData chestplate = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData leggings = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData boots = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData mainHand = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData offHand = new NpcEquipmentSlotData();

    public float resolvedScale() {
        if (scale > 0.0F && Math.abs(scale - 1.0F) > 0.001F) {
            return scale;
        }
        if (small) {
            return 0.5F;
        }
        return 1.0F;
    }

    public boolean isPacketMob() {
        return NpcEntityTypeResolver.isPacketMob(this);
    }

    public String resolvedEntityType() {
        return NpcEntityTypeResolver.resolveEntityId(this);
    }

    public void migrateLegacyHologramOffsets() {
        if (hologramBaseOffset <= 0.0F && nameDisplayOffset > 0.0F) {
            hologramBaseOffset = nameDisplayOffset;
        }
    }

    public NpcFoxVariant legacyFoxVariant() {
        return foxVariant == null ? NpcFoxVariant.RED : foxVariant;
    }

    public void ensureEquipmentSlots() {
        if (helmet == null) {
            helmet = new NpcEquipmentSlotData();
        }
        if (chestplate == null) {
            chestplate = new NpcEquipmentSlotData();
        }
        if (leggings == null) {
            leggings = new NpcEquipmentSlotData();
        }
        if (boots == null) {
            boots = new NpcEquipmentSlotData();
        }
        if (mainHand == null) {
            mainHand = new NpcEquipmentSlotData();
        }
        if (offHand == null) {
            offHand = new NpcEquipmentSlotData();
        }
    }
}
