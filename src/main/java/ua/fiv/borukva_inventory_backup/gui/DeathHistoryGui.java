package ua.fiv.borukva_inventory_backup.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import lombok.Setter;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.database.entities.DeathTable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.*;

@Setter
public class DeathHistoryGui extends SimpleGui {

    private int page;

    private List<DeathTable> deathTableList;

    public DeathHistoryGui(ServerPlayer player, int page, List<DeathTable> deathTables) {
        super(MenuType.GENERIC_9x6, player, false);

        this.deathTableList = deathTables;
        this.page = page;

        addButtons();

    }

    @Override
    public boolean canPlayerClose() {
        return true;
    }


    private void addButtons(){
        int firstIndex = this.page * 45;
        int tableSize = this.deathTableList.size();
        int lastIndex = Math.min(firstIndex + 45, tableSize);


        for(int i=firstIndex; i<lastIndex; i++){
            int inventory_index = i-firstIndex;

            if(inventory_index>44) break;
            //System.out.println("Size: "+tableSize+" Ref: "+(tableSize-i));
            String inventory = this.deathTableList.get(tableSize-i-1).getInventory();
            String armor = this.deathTableList.get(tableSize-i-1).getArmor();
            String offHand = this.deathTableList.get(tableSize-i-1).getOffHand();
            String enderChest = this.deathTableList.get(tableSize-i-1).getEnderChest();
            String trinkets = this.deathTableList.get(tableSize-i-1).getTrinkets();

            int xp = this.deathTableList.get(tableSize-i-1).getXp();

            this.setSlot(inventory_index, new GuiElementBuilder(Items.CHEST)
                    .setName(Component.literal("Time: "+this.deathTableList.get(tableSize-i-1).getDate()))
                    .addLoreLine(Component.literal("Death reason: "+this.deathTableList.get(tableSize-i-1).getReason()))
                    .addLoreLine(Component.literal("World: "+this.deathTableList.get(tableSize-i-1).getWorld()))
                    .addLoreLine(Component.literal("Place: "+this.deathTableList.get(tableSize-i-1).getPlace()))
                    .addLoreLine(Component.literal("XpLevel: "+this.deathTableList.get(tableSize-i-1).getXp()))
                    .setCallback(() -> {
                        Map<Integer, ItemStack> itemStackList = TableListGui.inventorySerialization(inventory, armor, offHand, player);

                        if(itemStackList.isEmpty()){
                            ModInit.LOGGER.error("Can't create InventoryGUI because itemStackList is null");
                            return;
                        }
                        new InventoryGui(player, this.deathTableList.getFirst().getName(), itemStackList, enderChest, trinkets, xp, this).open();
                    })
                    .build());

        }

        if (lastIndex < this.deathTableList.size()) {
            this.setSlot(53, new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal("Next Page"))
                    .setCallback(() -> new DeathHistoryGui(player, page + 1, this.deathTableList).open())
                    .build());
        }

        this.setSlot(49, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("Back to tables list"))
                .setCallback(() -> new TableListGui(player, deathTableList.getFirst().getName()).open())
                .build());

        if (page > 0) {
            this.setSlot(45, new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal("Previous Page"))
                    .setCallback(() -> new DeathHistoryGui(player, page - 1, this.deathTableList).open())
                    .build());
        }
//        System.out.println(TableListGui.activeTables);
    }

}


