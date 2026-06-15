package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import org.bukkit.Material;

@Comment(value = {
        @CommentValue(" Один слот экипировки player NPC"),
        @CommentValue(" material: AIR = пусто; для головы можно head-texture (base64 / URL)")
})
public final class NpcEquipmentSlotData {

    @Comment(value = {
            @CommentValue(" Bukkit-материал (AIR = слот пуст)")
    })
    public Material material = Material.AIR;

    @Comment(value = {
            @CommentValue(" Custom Model Data (resource pack)")
    })
    public int customModelData = 0;

    @Comment(value = {
            @CommentValue(" ItemsAdder ID (пусто = не используется)")
    })
    public String itemsAdderId = "";

    @Comment(value = {
            @CommentValue(" Nexo ID (пусто = не используется)")
    })
    public String nexoId = "";

    @Comment(value = {
            @CommentValue(" Текстура головы для слота helmet (base64 или URL)")
    })
    public String headTexture = "";

    public boolean isEmpty() {
        return material == null || material.isAir();
    }
}
