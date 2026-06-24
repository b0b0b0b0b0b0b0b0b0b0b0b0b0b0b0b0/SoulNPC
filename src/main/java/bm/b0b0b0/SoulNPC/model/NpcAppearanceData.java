package bm.b0b0b0.SoulNPC.model;

import bm.b0b0b0.SoulNPC.mob.NpcEntityTypeResolver;
import bm.b0b0b0.SoulNPC.appearance.NpcGlowColors;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Deprecated
    public NpcFoxVariant foxVariant = NpcFoxVariant.RED;

    @NewLine
    @Comment(value = {
            @CommentValue(" Голограмма (TextDisplay) — строки над NPC, MiniMessage"),
            @CommentValue(" Пример: \"<gradient:#7C3AED:#A855F7>Имя</gradient>\"")
    })
    public String name = "<white>NPC</white>";

    @Comment(value = {
            @CommentValue(" Не показывать строку name в голограмме (текст сохраняется)")
    })
    public boolean nameHidden = false;

    @Comment(value = {
            @CommentValue(" Скин player NPC: ник, имя скина SkinRestorer (/skin set), URL"),
            @CommentValue(" Без SkinRestorer — только Mojang по нику")
    })
    public String profile = "";

    @Comment(value = {
            @CommentValue(" Источник скина: NICK, URL, FILE, MINESKIN_ID")
    })
    public NpcSkinSource skinSource = NpcSkinSource.NICK;

    @Comment(value = {
            @CommentValue(" URL скина (skinSource: URL)")
    })
    public String skinUrl = "";

    @Comment(value = {
            @CommentValue(" Файл скина в plugins/SoulNPC/skins/ (skinSource: FILE)")
    })
    public String skinFile = "";

    @Comment(value = {
            @CommentValue(" Маска частей скина (-1 = все; иначе byte mask)")
    })
    public int skinLayers = -1;

    @Comment(value = {
            @CommentValue(" Устарело: текст только через TextDisplay (значение в yaml игнорируется)")
    })
    public boolean useTextDisplay = true;

    @Comment(value = {
            @CommentValue(" Высота центра нижней строки голограммы от ног (блоки)")
    })
    public float hologramBaseOffset = 2.25F;

    @Comment(value = {
            @CommentValue(" Расстояние между строками голограммы (блоки)")
    })
    public float hologramLineSpacing = 0.10F;

    @Comment(value = {
            @CommentValue(" Примерная высота одной строки для стека (блоки)")
    })
    public float hologramLineHeight = 0.26F;

    @Comment(value = {
            @CommentValue(" Масштаб строки name в голограмме")
    })
    public float nameDisplayScale = 1.0F;

    @Comment(value = {
            @CommentValue(" Масштаб extra-lines (доп. строки голограммы)")
    })
    public float descriptionDisplayScale = 0.85F;

    @Comment(value = {
            @CommentValue(" TextDisplay: текст виден сквозь блоки (false — скрывается за стенами, дефолт)")
    })
    public boolean hologramSeeThrough = false;

    @Comment(value = {
            @CommentValue(" TextDisplay: тень текста")
    })
    public boolean hologramShadowed = true;

    @Comment(value = {
            @CommentValue(" TextDisplay: полупрозрачный фон за текстом")
    })
    public boolean hologramBackground = false;

    @Comment(value = {
            @CommentValue(" Доп. строки голограммы (настраиваются в GUI → «Строки»)")
    })
    public List<NpcHologramLineData> extraLines = new ArrayList<>();

    @Deprecated
    public float nameDisplayOffset = 2.25F;

    @NewLine
    @Comment(value = {
            @CommentValue(" Свечение контура модели (entity flag + цвет scoreboard team)")
    })
    public boolean glow = false;

    @Comment(value = {
            @CommentValue(" Цвет свечения: white, yellow, aqua, red, … (см. gui/glow-menu)")
    })
    public String glowColor = "white";

    @Comment(value = {
            @CommentValue(" Размер модели (1.0 = обычный; 0.5 ≈ small)")
    })
    public float scale = 1.0F;

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
            @CommentValue(" Игроки упираются в NPC (scoreboard collision); false — можно пройти сквозь")
    })
    public boolean collidable = false;

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

    @NewLine
    @Comment(value = {
            @CommentValue(" MOB: свойства entity metadata (baby, fox_variant, sheep_color, …)")
    })
    public Map<String, String> mobProperties = new HashMap<>();

    @Comment(value = {
            @CommentValue(" MOB: экипировка (packet entity equipment)")
    })
    public NpcMobEquipmentData mobEquipment = new NpcMobEquipmentData();

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

    public void normalizePresentation() {
        useTextDisplay = true;
        glowColor = NpcGlowColors.normalizeId(glowColor);
        if (extraLines == null) {
            extraLines = new ArrayList<>();
        }
    }

    public void migrateLegacyDescription(String legacy) {
        if (legacy == null || legacy.isBlank()) {
            return;
        }
        ensureExtraLines();
        if (extraLines.isEmpty()) {
            extraLines.add(NpcHologramLineData.of(legacy));
        }
    }

    public void ensureExtraLines() {
        if (extraLines == null) {
            extraLines = new ArrayList<>();
        }
    }

    public void ensureExtraLine(int extraIndex) {
        ensureExtraLines();
        while (extraLines.size() <= extraIndex) {
            extraLines.add(new NpcHologramLineData());
        }
        if (extraLines.get(extraIndex) == null) {
            extraLines.set(extraIndex, new NpcHologramLineData());
        }
    }

    public void migrateLegacyExtraLineStrings(java.util.List<String> legacyLines) {
        if (legacyLines == null || legacyLines.isEmpty()) {
            return;
        }
        ensureExtraLines();
        if (!extraLines.isEmpty()) {
            return;
        }
        for (String line : legacyLines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            extraLines.add(NpcHologramLineData.of(line));
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

    public void ensureMobEquipment() {
        if (mobEquipment == null) {
            mobEquipment = new NpcMobEquipmentData();
        }
        mobEquipment.ensureSlots();
        if (mobProperties == null) {
            mobProperties = new HashMap<>();
        }
    }
}
