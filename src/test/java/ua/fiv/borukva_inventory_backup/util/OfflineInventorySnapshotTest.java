package ua.fiv.borukva_inventory_backup.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OfflineInventorySnapshotTest {
    @Test
    void snapshotsSparse26_2MainInventoryEquipmentAndOffhandWithoutMovingSlots() throws Exception {
        ListTag storedInventory = new ListTag();
        storedInventory.add(item("minecraft:stone", 12, 0));
        storedInventory.add(item("minecraft:diamond", 3, 35));

        CompoundTag equipment = new CompoundTag();
        equipment.put("feet", item("minecraft:diamond_boots", 1));
        equipment.put("head", item("minecraft:carved_pumpkin", 1));
        equipment.put("offhand", item("minecraft:shield", 1));

        OfflineInventorySnapshot snapshot = OfflineInventorySnapshot.fromPlayerData(
                storedInventory, equipment
        );

        ListTag main = parseList(snapshot.inventory());
        ListTag armor = parseList(snapshot.armor());
        ListTag offHand = parseList(snapshot.offHand());

        assertEquals(36, main.size());
        assertItem(main.getCompound(0).orElseThrow(), "minecraft:stone", 12);
        assertItem(main.getCompound(1).orElseThrow(), "minecraft:air", 0);
        assertItem(main.getCompound(35).orElseThrow(), "minecraft:diamond", 3);

        assertEquals(4, armor.size());
        assertItem(armor.getCompound(0).orElseThrow(), "minecraft:diamond_boots", 1);
        assertItem(armor.getCompound(1).orElseThrow(), "minecraft:air", 0);
        assertItem(armor.getCompound(2).orElseThrow(), "minecraft:air", 0);
        assertItem(armor.getCompound(3).orElseThrow(), "minecraft:carved_pumpkin", 1);

        assertEquals(1, offHand.size());
        assertItem(offHand.getCompound(0).orElseThrow(), "minecraft:shield", 1);
    }

    @Test
    void retainsLegacyArmorAndOffhandSlotsForUnmigratedPlayerFiles() throws Exception {
        ListTag storedInventory = new ListTag();
        storedInventory.add(item("minecraft:apple", 2, 7));
        storedInventory.add(item("minecraft:iron_boots", 1, 100));
        storedInventory.add(item("minecraft:iron_helmet", 1, 103));
        storedInventory.add(item("minecraft:torch", 16, -106));

        OfflineInventorySnapshot snapshot = OfflineInventorySnapshot.fromPlayerData(
                storedInventory, new CompoundTag()
        );

        ListTag main = parseList(snapshot.inventory());
        ListTag armor = parseList(snapshot.armor());
        ListTag offHand = parseList(snapshot.offHand());
        assertItem(main.getCompound(7).orElseThrow(), "minecraft:apple", 2);
        assertItem(armor.getCompound(0).orElseThrow(), "minecraft:iron_boots", 1);
        assertItem(armor.getCompound(3).orElseThrow(), "minecraft:iron_helmet", 1);
        assertItem(offHand.getCompound(0).orElseThrow(), "minecraft:torch", 16);
    }

    @Test
    void normalizesSparseEnderChestSlotsForRecoverablePreRestoreSnapshots() throws Exception {
        ListTag storedEnderChest = new ListTag();
        storedEnderChest.add(item("minecraft:ender_pearl", 4, 2));
        storedEnderChest.add(item("minecraft:elytra", 1, 26));

        ListTag normalized = parseList(
                OfflineInventorySnapshot.normalizeSlottedContainer(storedEnderChest, 27)
        );

        assertEquals(27, normalized.size());
        assertItem(normalized.getCompound(0).orElseThrow(), "minecraft:air", 0);
        assertItem(normalized.getCompound(2).orElseThrow(), "minecraft:ender_pearl", 4);
        assertItem(normalized.getCompound(26).orElseThrow(), "minecraft:elytra", 1);
    }

    private static ListTag parseList(String list) throws Exception {
        return TagParser.parseCompoundFully("{Inventory:" + list + "}")
                .getList("Inventory")
                .orElseThrow();
    }

    private static CompoundTag item(String id, int count, int slot) {
        CompoundTag item = item(id, count);
        item.putByte("Slot", (byte) slot);
        return item;
    }

    private static CompoundTag item(String id, int count) {
        CompoundTag item = new CompoundTag();
        item.putString("id", id);
        item.putInt("count", count);
        return item;
    }

    private static void assertItem(CompoundTag item, String id, int count) {
        assertEquals(id, item.getString("id").orElseThrow());
        assertEquals(count, item.getInt("count").orElseThrow());
        assertFalse(item.contains("Slot"));
    }
}
