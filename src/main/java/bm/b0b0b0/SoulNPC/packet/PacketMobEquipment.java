package bm.b0b0b0.SoulNPC.packet;

import bm.b0b0b0.SoulNPC.appearance.ItemStackFactory;
import bm.b0b0b0.SoulNPC.model.NpcAppearanceData;
import bm.b0b0b0.SoulNPC.model.NpcEquipmentSlotData;
import bm.b0b0b0.SoulNPC.model.NpcMobEquipmentData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import java.util.ArrayList;
import java.util.List;

public final class PacketMobEquipment {

    private PacketMobEquipment() {
    }

    public static void apply(Object channel, int entityId, NpcAppearanceData appearance, ItemStackFactory factory) {
        if (channel == null || appearance == null || factory == null || !appearance.isPacketMob()) {
            return;
        }
        appearance.ensureMobEquipment();
        List<Equipment> equipment = buildEquipment(appearance.mobEquipment, factory);
        if (equipment.isEmpty()) {
            return;
        }
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(entityId, equipment);
        PacketEvents.getAPI().getProtocolManager().sendPacket(channel, packet);
    }

    public static void refresh(Object channel, int entityId, NpcAppearanceData appearance, ItemStackFactory factory) {
        apply(channel, entityId, appearance, factory);
    }

    private static List<Equipment> buildEquipment(NpcMobEquipmentData equipmentData, ItemStackFactory factory) {
        equipmentData.ensureSlots();
        List<Equipment> equipment = new ArrayList<>(6);
        addSlot(equipment, EquipmentSlot.MAIN_HAND, equipmentData.mainHand, factory);
        addSlot(equipment, EquipmentSlot.OFF_HAND, equipmentData.offHand, factory);
        addSlot(equipment, EquipmentSlot.HELMET, equipmentData.helmet, factory);
        addSlot(equipment, EquipmentSlot.CHEST_PLATE, equipmentData.chestplate, factory);
        addSlot(equipment, EquipmentSlot.LEGGINGS, equipmentData.leggings, factory);
        addSlot(equipment, EquipmentSlot.BOOTS, equipmentData.boots, factory);
        return equipment;
    }

    private static void addSlot(
            List<Equipment> equipment,
            EquipmentSlot slot,
            NpcEquipmentSlotData data,
            ItemStackFactory factory
    ) {
        org.bukkit.inventory.ItemStack bukkit = factory.create(data);
        if (bukkit == null || bukkit.isEmpty()) {
            return;
        }
        ItemStack packetItem = SpigotConversionUtil.fromBukkitItemStack(bukkit);
        equipment.add(new Equipment(slot, packetItem));
    }
}
