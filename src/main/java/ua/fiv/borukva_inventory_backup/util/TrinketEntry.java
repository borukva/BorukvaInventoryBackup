package ua.fiv.borukva_inventory_backup.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.gui.InventoryGui;

import java.util.ArrayList;
import java.util.List;

/**
 * One occupied trinket slot. Deliberately free of Trinkets classes, so stored
 * snapshots can be browsed and exported even on a server without the mod.
 * {@code slot} is the Trinkets slot id ({@code group/name}).
 */
public record TrinketEntry(String slot, int index, boolean cosmetic, ItemStack stack) {

    public static String serialize(List<TrinketEntry> entries, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TrinketEntry entry : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("slot", entry.slot());
            tag.putInt("index", entry.index());
            tag.putBoolean("cosmetic", entry.cosmetic());
            tag.put("item", InventoryGui.getItemStackNbt(entry.stack(), registries.createSerializationContext(NbtOps.INSTANCE)));
            list.add(tag);
        }
        return list.toString();
    }

    /**
     * Returns {@code null} when the snapshot carries no trinket data (recorded
     * before trinket support, without Trinkets installed, or unreadable), and
     * an empty list when the player simply wore no trinkets. Restores must leave
     * trinket slots untouched in the first case and clear them in the second.
     */
    public static @Nullable List<TrinketEntry> parse(@Nullable String serialized, HolderLookup.Provider registries) {
        if (serialized == null) {
            return null;
        }

        ListTag list;
        try {
            list = TagParser.parseCompoundFully("{Trinkets:" + serialized + "}").getListOrEmpty("Trinkets");
        } catch (CommandSyntaxException e) {
            ModInit.LOGGER.error("Unreadable trinkets snapshot: {}", e.getMessage());
            return null;
        }

        List<TrinketEntry> entries = new ArrayList<>();
        for (Tag element : list) {
            CompoundTag tag = element.asCompound().orElse(null);
            if (tag == null) {
                continue;
            }
            ItemStack stack = ItemStack.CODEC
                    .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompoundOrEmpty("item"))
                    .result()
                    .orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                continue;
            }
            entries.add(new TrinketEntry(
                    tag.getStringOr("slot", ""),
                    tag.getIntOr("index", 0),
                    tag.getBooleanOr("cosmetic", false),
                    stack
            ));
        }
        return entries;
    }
}
