package ua.fiv.borukva_inventory_backup.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/**
 * Converts the sparse inventory representation in a player data file into the
 * dense lists used by the backup UI. Minecraft 26.2 stores the main inventory
 * in {@code Inventory} and armor/offhand items in the {@code equipment}
 * compound. Legacy slot values are retained as a fallback for player files
 * that have not yet been rewritten by 26.2.
 */
public record OfflineInventorySnapshot(String inventory, String armor, String offHand) {
    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final int ARMOR_SIZE = 4;

    public static OfflineInventorySnapshot fromPlayerData(ListTag storedInventory, CompoundTag equipment) {
        ListTag mainInventory = emptyItems(MAIN_INVENTORY_SIZE);
        ListTag armor = emptyItems(ARMOR_SIZE);
        ListTag offHand = emptyItems(1);

        for (Tag element : storedInventory) {
            Optional<CompoundTag> item = element.asCompound();
            if (item.isEmpty()) {
                continue;
            }

            Optional<Byte> storedSlot = item.get().getByte("Slot");
            if (storedSlot.isEmpty()) {
                continue;
            }

            byte rawSlot = storedSlot.get();
            int slot = Byte.toUnsignedInt(rawSlot);
            if (slot < MAIN_INVENTORY_SIZE) {
                mainInventory.set(slot, itemWithoutSlot(item.get()));
            } else if (slot >= 100 && slot < 100 + ARMOR_SIZE) {
                armor.set(slot - 100, itemWithoutSlot(item.get()));
            } else if (rawSlot == (byte) -106) {
                offHand.set(0, itemWithoutSlot(item.get()));
            }
        }

        String[] armorSlots = {"feet", "legs", "chest", "head"};
        for (int slot = 0; slot < armorSlots.length; slot++) {
            int armorIndex = slot;
            equipment.getCompound(armorSlots[slot])
                    .ifPresent(item -> armor.set(armorIndex, itemWithoutSlot(item)));
        }
        equipment.getCompound("offhand")
                .ifPresent(item -> offHand.set(0, itemWithoutSlot(item)));

        return new OfflineInventorySnapshot(
                mainInventory.toString(), armor.toString(), offHand.toString()
        );
    }

    public static String normalizeSlottedContainer(ListTag storedItems, int size) {
        ListTag normalized = emptyItems(size);
        for (Tag element : storedItems) {
            Optional<CompoundTag> item = element.asCompound();
            if (item.isEmpty()) {
                continue;
            }

            Optional<Byte> storedSlot = item.get().getByte("Slot");
            if (storedSlot.isEmpty()) {
                continue;
            }

            int slot = Byte.toUnsignedInt(storedSlot.get());
            if (slot < size) {
                normalized.set(slot, itemWithoutSlot(item.get()));
            }
        }
        return normalized.toString();
    }

    private static ListTag emptyItems(int size) {
        ListTag items = new ListTag();
        for (int slot = 0; slot < size; slot++) {
            CompoundTag emptyItem = new CompoundTag();
            emptyItem.putInt("count", 0);
            emptyItem.putString("id", "minecraft:air");
            items.add(emptyItem);
        }
        return items;
    }

    private static CompoundTag itemWithoutSlot(CompoundTag item) {
        CompoundTag copy = item.copy();
        copy.remove("Slot");
        return copy;
    }
}
