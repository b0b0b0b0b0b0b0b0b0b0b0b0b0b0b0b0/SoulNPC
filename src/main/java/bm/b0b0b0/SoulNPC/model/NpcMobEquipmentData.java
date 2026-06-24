package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

@Comment(value = {
        @CommentValue(" Экипировка mob NPC (packet entity equipment)")
})
public final class NpcMobEquipmentData {

    public NpcEquipmentSlotData mainHand = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData offHand = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData helmet = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData chestplate = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData leggings = new NpcEquipmentSlotData();
    public NpcEquipmentSlotData boots = new NpcEquipmentSlotData();

    public void ensureSlots() {
        if (mainHand == null) {
            mainHand = new NpcEquipmentSlotData();
        }
        if (offHand == null) {
            offHand = new NpcEquipmentSlotData();
        }
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
    }
}
