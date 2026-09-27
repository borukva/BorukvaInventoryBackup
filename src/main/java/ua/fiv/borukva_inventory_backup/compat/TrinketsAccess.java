package ua.fiv.borukva_inventory_backup.compat;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ua.fiv.borukva_inventory_backup.util.TrinketEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct Trinkets API calls. Must only be reached through {@link TrinketsCompat}.
 * Cosmetic slots only exist when the slot type supports them and the server
 * has them enabled; otherwise cosmetic entries fall back to the inventory.
 */
final class TrinketsAccess {
    private TrinketsAccess() {}

    static String capture(ServerPlayer player) {
        List<TrinketEntry> entries = new ArrayList<>();

        for (TrinketInventory inventory : TrinketsApi.getAttachment(player).getInventories().values()) {
            String slot = inventory.slotType().getId();
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                ItemStack stack = inventory.getItem(index);
                if (!stack.isEmpty()) {
                    entries.add(new TrinketEntry(slot, index, false, stack));
                }
                ItemStack cosmetic = inventory.getCosmeticItem(index);
                if (!cosmetic.isEmpty()) {
                    entries.add(new TrinketEntry(slot, index, true, cosmetic));
                }
            }
        }

        return TrinketEntry.serialize(entries, player.registryAccess());
    }

    static List<ItemStack> restore(ServerPlayer player, List<TrinketEntry> entries) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);

        for (TrinketInventory inventory : attachment.getInventories().values()) {
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                inventory.setItem(index, ItemStack.EMPTY);
                inventory.setCosmeticItem(index, ItemStack.EMPTY);
            }
        }

        List<ItemStack> leftovers = new ArrayList<>();
        for (TrinketEntry entry : entries) {
            if (!place(attachment.getInventory(entry.slot()), entry)) {
                leftovers.add(entry.stack());
            }
        }
        return leftovers;
    }

    private static boolean place(TrinketInventory inventory, TrinketEntry entry) {
        if (inventory == null || !inventory.isValidSlot(entry.index())
                || entry.index() >= inventory.getContainerSize()) {
            return false;
        }
        if (entry.cosmetic()) {
            return inventory.getCosmeticItem(entry.index()).isEmpty()
                    && inventory.setCosmeticItem(entry.index(), entry.stack().copy());
        }
        if (!inventory.getItem(entry.index()).isEmpty()) {
            return false;
        }
        inventory.setItem(entry.index(), entry.stack().copy());
        return true;
    }
}
