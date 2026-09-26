package ua.fiv.borukva_inventory_backup.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import ua.fiv.borukva_inventory_backup.util.TrinketEntry;

import java.util.List;

/** Read-only view of a snapshot's trinkets; restoring happens from {@link InventoryGui}. */
public class TrinketsGui extends SimpleGui {

    public TrinketsGui(ServerPlayer player, List<TrinketEntry> trinkets, SimpleGui caller) {
        super(MenuType.GENERIC_9x6, player, false);

        for (int i = 0; i < Math.min(trinkets.size(), 45); i++) {
            TrinketEntry entry = trinkets.get(i);
            this.setSlot(i, new GuiElementBuilder(entry.stack())
                    .setCount(entry.stack().getCount())
                    .addLoreLine(Component.literal("Slot: " + entry.slot() + " #" + entry.index() + (entry.cosmetic() ? " (cosmetic)" : ""))
                            .withStyle(ChatFormatting.GOLD))
                    .build());
        }

        if (trinkets.size() > 45) {
            this.setSlot(49, new GuiElementBuilder(Items.PAPER)
                    .setName(Component.literal((trinkets.size() - 45) + " more trinkets are not shown, but will be restored").withStyle(ChatFormatting.YELLOW))
                    .build());
        }

        this.setSlot(45, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("Return back"))
                .setCallback(() -> caller.open())
                .build());
    }
}
