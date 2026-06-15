package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEquipmentSlotData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.npc.NPC;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

public final class PacketPlayerEquipment {

    private PacketPlayerEquipment() {
    }

    public static void apply(NPC npc, NpcAppearanceData appearance, ItemStackFactory factory) {
        if (npc == null || appearance == null || !appearance.type.isPlayerModel() || factory == null) {
            return;
        }
        appearance.ensureEquipmentSlots();
        npc.setMainHand(toPacket(appearance.mainHand, factory));
        npc.setOffHand(toPacket(appearance.offHand, factory));
        npc.setHelmet(toPacket(appearance.helmet, factory));
        npc.setChestplate(toPacket(appearance.chestplate, factory));
        npc.setLeggings(toPacket(appearance.leggings, factory));
        npc.setBoots(toPacket(appearance.boots, factory));
    }

    public static void refresh(NPC npc) {
        if (npc == null || npc.getChannels().isEmpty()) {
            return;
        }
        npc.updateEquipment();
    }

    private static ItemStack toPacket(NpcEquipmentSlotData slot, ItemStackFactory factory) {
        org.bukkit.inventory.ItemStack bukkit = factory.create(slot);
        if (bukkit == null || bukkit.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return SpigotConversionUtil.fromBukkitItemStack(bukkit);
    }
}
