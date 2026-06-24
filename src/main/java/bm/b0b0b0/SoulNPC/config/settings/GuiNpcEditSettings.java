package bm.b0b0b0.SoulNPC.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class GuiNpcEditSettings extends YamlSerializable {

    @NewLine
    @Comment(value = {
            @CommentValue(" GUI редактирования NPC (голограмма / TextDisplay)")
    })
    public Layout layout = new Layout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подменю «Спавн предметов»")
    })
    public GroundItemsLayout groundItemsMenu = new GroundItemsLayout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подтверждение удаления NPC")
    })
    public DeleteConfirmLayout deleteConfirmMenu = new DeleteConfirmLayout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подменю «Одеть NPC» (экипировка player-модели)")
    })
    public DressLayout dressMenu = new DressLayout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подменю «Свечение» (вкл/выкл + цвет контура)")
    })
    public GlowLayout glowMenu = new GlowLayout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подменю «Строки голограммы»")
    })
    public LinesLayout linesMenu = new LinesLayout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подтверждение удаления строки голограммы")
    })
    public LineDeleteConfirmLayout lineDeleteConfirmMenu = new LineDeleteConfirmLayout();

    @NewLine
    @Comment(value = {
            @CommentValue(" Подменю «Действия при клике»")
    })
    public ActionsLayout actionsMenu = new ActionsLayout();

    public GuiNpcEditSettings() {
        super(SoulNpcSerializerConfig.INSTANCE);
    }

    public static final class Layout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int hologramOffsetSlot = 10;
        public int hologramSpacingSlot = 11;
        public int hologramScaleSlot = 12;

        public int packetViewDistanceSlot = 13;
        public int hologramViewDistanceSlot = 14;
        public int visibilityPermissionSlot = 15;

        public int groundItemsOpenSlot = 16;
        public int dressOpenSlot = 25;
        public int glowOpenSlot = 23;
        public int linesOpenSlot = 44;
        public int actionsOpenSlot = 43;

        public int seeThroughSlot = 20;
        public int shadowSlot = 21;
        public int backgroundSlot = 22;
        public int collidableSlot = 24;
        public int teleportToMeSlot = 28;
        public int lookAtPlayersSlot = 29;
        public int entityPoseSlot = 31;

        public int respawnSlot = 30;
        public int deleteSlot = 32;
        public int backSlot = 36;

        public Material hologramOffsetMaterial = Material.LADDER;
        public Material hologramSpacingMaterial = Material.IRON_TRAPDOOR;
        public Material hologramScaleMaterial = Material.MAGMA_CREAM;
        public Material packetViewDistanceMaterial = Material.SPYGLASS;
        public Material hologramViewDistanceMaterial = Material.ENDER_EYE;
        public Material visibilityPermissionMaterial = Material.OAK_SIGN;
        public Material groundItemsOpenMaterial = Material.CHEST;
        public Material dressOpenMaterial = Material.DIAMOND_CHESTPLATE;
        public Material glowOpenMaterial = Material.OAK_SIGN;
        public Material linesOpenMaterial = Material.WHITE_BANNER;
        public Material actionsOpenMaterial = Material.COMMAND_BLOCK;
        public Material seeThroughMaterial = Material.ENDER_EYE;
        public Material shadowMaterial = Material.TORCH;
        public Material backgroundMaterial = Material.BLACK_STAINED_GLASS_PANE;
        public Material collidableMaterial = Material.SLIME_BLOCK;
        public Material teleportToMeMaterial = Material.ENDER_PEARL;
        public Material lookAtPlayersMaterial = Material.PLAYER_HEAD;
        public Material entityPoseMaterial = Material.ARMOR_STAND;
        public Material respawnMaterial = Material.NETHER_STAR;
        public Material deleteMaterial = Material.BARRIER;
        public Material backMaterial = Material.LIGHT_GRAY_DYE;
    }

    public static final class ActionsLayout {
        public int size = 54;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int backSlot = 45;
        public int addSlot = 49;

        public Material backMaterial = Material.LIGHT_GRAY_DYE;
        public Material addMaterial = Material.LIME_DYE;
        public Material actionMaterial = Material.PAPER;
    }

    public static final class LinesLayout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int nameHideSlot = 4;
        public int backSlot = 36;

        public int lineSlot0 = 19;
        public int lineSlot1 = 20;
        public int lineSlot2 = 21;
        public int lineSlot3 = 22;
        public int lineSlot4 = 23;
        public int lineSlot5 = 24;
        public int lineSlot6 = 25;
        public int lineSlot7 = 28;
        public int lineSlot8 = 29;
        public int lineSlot9 = 30;
        public int lineSlot10 = 31;
        public int lineSlot11 = 32;
        public int lineSlot12 = 33;
        public int lineSlot13 = 34;

        public Material backMaterial = Material.LIGHT_GRAY_DYE;

        public int[] lineSlots() {
            return new int[]{
                    lineSlot0, lineSlot1, lineSlot2, lineSlot3, lineSlot4, lineSlot5, lineSlot6,
                    lineSlot7, lineSlot8, lineSlot9, lineSlot10, lineSlot11, lineSlot12, lineSlot13
            };
        }
    }

    public static final class LineDeleteConfirmLayout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int infoSlot = 4;
        public int yesSlot = 11;
        public int noSlot = 15;
        public int backSlot = 36;

        public Material infoMaterial = Material.PAPER;
        public Material yesMaterial = Material.LIME_DYE;
        public Material noMaterial = Material.RED_DYE;
        public Material backMaterial = Material.LIGHT_GRAY_DYE;
    }

    public static final class GroundItemsLayout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int toggleSlot = 11;
        public int offsetSlot = 10;
        public int intervalSlot = 14;
        public int burstSlot = 15;
        public int backSlot = 36;

        public Material toggleMaterial = Material.GOLD_INGOT;
        public Material offsetMaterial = Material.FEATHER;
        public Material burstMaterial = Material.FIREWORK_ROCKET;
        public Material intervalMaterial = Material.CLOCK;
        public Material backMaterial = Material.LIGHT_GRAY_DYE;
    }

    public static final class DeleteConfirmLayout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int infoSlot = 4;
        public int yesSlot = 11;
        public int noSlot = 15;
        public int backSlot = 36;

        public Material infoMaterial = Material.PAPER;
        public Material yesMaterial = Material.LIME_DYE;
        public Material noMaterial = Material.RED_DYE;
        public Material backMaterial = Material.LIGHT_GRAY_DYE;
    }

    public static final class DressLayout {
        public int size = 54;
        public Material fillerMaterial = Material.BLACK_STAINED_GLASS_PANE;

        public int helmetSlot = 13;
        public int chestSlot = 22;
        public int leggingsSlot = 31;
        public int bootsSlot = 40;
        public int offHandSlot = 21;
        public int mainHandSlot = 23;
        public int backSlot = 36;

        public Material helmetHintMaterial = Material.CHAINMAIL_HELMET;
        public Material chestHintMaterial = Material.CHAINMAIL_CHESTPLATE;
        public Material leggingsHintMaterial = Material.CHAINMAIL_LEGGINGS;
        public Material bootsHintMaterial = Material.CHAINMAIL_BOOTS;
        public Material mainHandHintMaterial = Material.IRON_SWORD;
        public Material offHandHintMaterial = Material.SHIELD;
        public Material backMaterial = Material.LIGHT_GRAY_DYE;

        public int[] equipmentSlots() {
            return new int[]{helmetSlot, chestSlot, leggingsSlot, bootsSlot, offHandSlot, mainHandSlot};
        }
    }

    public static final class GlowLayout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int disableSlot = 26;
        public int backSlot = 36;

        public int colorSlot0 = 10;
        public int colorSlot1 = 11;
        public int colorSlot2 = 12;
        public int colorSlot3 = 13;
        public int colorSlot4 = 14;
        public int colorSlot5 = 15;
        public int colorSlot6 = 16;
        public int colorSlot7 = 19;
        public int colorSlot8 = 20;
        public int colorSlot9 = 21;
        public int colorSlot10 = 22;
        public int colorSlot11 = 23;
        public int colorSlot12 = 24;
        public int colorSlot13 = 25;
        public int colorSlot14 = 28;
        public int colorSlot15 = 29;
        public int colorSlot16 = 30;
        public int colorSlot17 = 31;
        public int colorSlot18 = 32;
        public int colorSlot19 = 33;
        public int colorSlot20 = 34;

        public Material disableMaterial = Material.BARRIER;
        public Material backMaterial = Material.LIGHT_GRAY_DYE;

        public int[] colorSlots() {
            return new int[]{
                    colorSlot0, colorSlot1, colorSlot2, colorSlot3, colorSlot4, colorSlot5, colorSlot6,
                    colorSlot7, colorSlot8, colorSlot9, colorSlot10, colorSlot11, colorSlot12, colorSlot13,
                    colorSlot14, colorSlot15, colorSlot16, colorSlot17, colorSlot18, colorSlot19, colorSlot20
            };
        }
    }
}
