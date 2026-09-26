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
 * Built against the Trinkets Updated release running on the server, which has
 * no cosmetic slots yet; entries are still marked {@code cosmetic} in the
 * stored format so newer releases can be supported without migrating data.
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
            }
        }

        return TrinketEntry.serialize(entries, player.registryAccess());
    }

    static List<ItemStack> restore(ServerPlayer player, List<TrinketEntry> entries) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);

        for (TrinketInventory inventory : attachment.getInventories().values()) {
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                inventory.setItem(index, ItemStack.EMPTY);
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
        if (entry.cosmetic() || inventory == null || !inventory.isValidSlot(entry.index())
                || entry.index() >= inventory.getContainerSize() || !inventory.getItem(entry.index()).isEmpty()) {
            return false;
        }
        inventory.setItem(entry.index(), entry.stack().copy());
        return true;
    }
}
