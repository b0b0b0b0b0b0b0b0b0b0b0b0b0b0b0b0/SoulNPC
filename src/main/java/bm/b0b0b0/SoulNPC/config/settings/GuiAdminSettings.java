package bm.b0b0b0.SoulNPC.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

public final class GuiAdminSettings extends YamlSerializable {

    @NewLine
    @Comment(value = {
            @CommentValue(" GUI редактора NPC")
    })
    public Layout layout = new Layout();

    public GuiAdminSettings() {
        super(SoulNpcSerializerConfig.INSTANCE);
    }

    public static final class Layout {
        public int size = 54;
        public int npcListStart = 0;
        public int npcListEnd = 44;
        @Comment(value = {
                @CommentValue(" create-slot — предыдущая страница, close-slot — следующая")
        })
        public int createSlot = 45;
        public int reloadSlot = 49;
        public int closeSlot = 53;
        public Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
        public Material createMaterial = Material.EMERALD;
        public Material reloadMaterial = Material.COMPARATOR;
        public Material closeMaterial = Material.BARRIER;
        public Material npcMaterial = Material.PLAYER_HEAD;
    }
}
