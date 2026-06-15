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

    public GuiNpcEditSettings() {
        super(SoulNpcSerializerConfig.INSTANCE);
    }

    public static final class Layout {
        public int size = 45;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;

        public int offsetUpSlot = 10;
        public int offsetDownSlot = 11;
        public int spacingUpSlot = 12;
        public int spacingDownSlot = 13;
        public int scaleUpSlot = 14;
        public int scaleDownSlot = 15;

        public int groundItemsSlot = 16;
        public int groundItemOffsetUpSlot = 18;
        public int groundItemOffsetDownSlot = 19;
        public int groundItemBurstSlot = 24;
        public int groundItemIntervalFasterSlot = 25;
        public int groundItemIntervalSlowerSlot = 27;

        public int seeThroughSlot = 20;
        public int shadowSlot = 21;
        public int backgroundSlot = 22;
        public int textDisplaySlot = 23;
        public int teleportToMeSlot = 28;
        public int lookAtPlayersSlot = 29;
        public int entityPoseSlot = 31;

        public int lineNameSlot = 44;
        public int lineDescriptionSlot = 35;
        public int lineExtraSlot0 = 26;
        public int lineExtraSlot1 = 17;
        public int lineExtraSlot2 = 8;
        public int lineAddSlot = 7;

        public int respawnSlot = 30;
        public int deleteSlot = 32;
        public int backSlot = 36;

        public Material offsetUpMaterial = Material.LADDER;
        public Material offsetDownMaterial = Material.SCAFFOLDING;
        public Material spacingUpMaterial = Material.IRON_TRAPDOOR;
        public Material spacingDownMaterial = Material.OAK_TRAPDOOR;
        public Material scaleUpMaterial = Material.MAGMA_CREAM;
        public Material scaleDownMaterial = Material.SLIME_BALL;
        public Material groundItemsMaterial = Material.GOLD_INGOT;
        public Material groundItemOffsetUpMaterial = Material.LADDER;
        public Material groundItemOffsetDownMaterial = Material.SCAFFOLDING;
        public Material groundItemBurstMaterial = Material.FIREWORK_ROCKET;
        public Material groundItemIntervalFasterMaterial = Material.SUGAR;
        public Material groundItemIntervalSlowerMaterial = Material.CLOCK;
        public Material seeThroughMaterial = Material.ENDER_EYE;
        public Material shadowMaterial = Material.TORCH;
        public Material backgroundMaterial = Material.BLACK_STAINED_GLASS_PANE;
        public Material textDisplayMaterial = Material.OAK_SIGN;
        public Material teleportToMeMaterial = Material.ENDER_PEARL;
        public Material lookAtPlayersMaterial = Material.PLAYER_HEAD;
        public Material entityPoseMaterial = Material.ARMOR_STAND;
        public Material lineNameMaterial = Material.LIGHT_BLUE_STAINED_GLASS;
        public Material lineDescriptionMaterial = Material.CYAN_STAINED_GLASS;
        public Material lineExtraMaterial = Material.WHITE_STAINED_GLASS;
        public Material lineAddMaterial = Material.LIME_STAINED_GLASS;
        public Material respawnMaterial = Material.NETHER_STAR;
        public Material deleteMaterial = Material.BARRIER;
        public Material backMaterial = Material.ARROW;

        public int[] lineExtraSlots() {
            return new int[]{lineExtraSlot0, lineExtraSlot1, lineExtraSlot2};
        }
    }
}
