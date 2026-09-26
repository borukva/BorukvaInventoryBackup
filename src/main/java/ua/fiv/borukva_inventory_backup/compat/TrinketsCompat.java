package ua.fiv.borukva_inventory_backup.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import ua.fiv.borukva_inventory_backup.util.TrinketEntry;

import java.util.List;

/**
 * Entry point for everything Trinkets-related. Trinkets is optional: the
 * classes that touch its API live in {@link TrinketsAccess} and are only
 * loaded when the mod is present.
 */
public final class TrinketsCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("trinkets");

    private TrinketsCompat() {}

    /** Serialized trinkets of the player, or {@code null} without Trinkets. Server thread only. */
    public static @Nullable String capture(ServerPlayer player) {
        return LOADED ? TrinketsAccess.capture(player) : null;
    }

    /**
     * Replaces the player's trinkets with {@code entries}. Anything that no
     * longer fits a slot (slot removed, fewer slots, no Trinkets on this server)
     * goes to the player's inventory, or drops at their feet when it is full.
     */
    public static void restore(ServerPlayer player, List<TrinketEntry> entries) {
        List<ItemStack> leftovers = LOADED
                ? TrinketsAccess.restore(player, entries)
                : entries.stream().map(TrinketEntry::stack).toList();

        for (ItemStack stack : leftovers) {
            player.getInventory().placeItemBackInInventory(stack.copy());
        }
    }
}
