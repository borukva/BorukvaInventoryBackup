package ua.fiv.borukva_inventory_backup.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySerializerTest {
    @Test
    void assignsSequentialSlotsAndNormalizesComponents() {
        CompoundTag result = InventorySerializer.deserializeInventory(
                "[{count:1,id:\"minecraft:stone\",components:{}}," +
                        "{count:2,id:\"minecraft:dirt\",components:{custom_value:1}}]"
        );

        ListTag inventory = result.getList("Inventory").orElseThrow();
        assertEquals(2, inventory.size());

        CompoundTag first = inventory.getCompoundOrEmpty(0);
        assertEquals((byte) 0, first.getByteOr("Slot", (byte) -1));
        assertFalse(first.contains("components"));

        CompoundTag second = inventory.getCompoundOrEmpty(1);
        assertEquals((byte) 1, second.getByteOr("Slot", (byte) -1));
        assertTrue(second.contains("components"));
    }

    @Test
    void leavesNonItemEntriesWithoutSlots() {
        CompoundTag result = InventorySerializer.deserializeInventory("[{count:1}]");

        CompoundTag entry = result.getList("Inventory").orElseThrow().getCompoundOrEmpty(0);
        assertFalse(entry.contains("Slot"));
    }

    @Test
    void malformedInputReturnsAnEmptyCompound() {
        assertTrue(InventorySerializer.deserializeInventory("[").isEmpty());
    }

}
