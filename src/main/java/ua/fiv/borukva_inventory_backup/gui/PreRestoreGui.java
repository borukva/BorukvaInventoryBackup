package ua.fiv.borukva_inventory_backup.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import lombok.Setter;
import ua.fiv.borukva_inventory_backup.database.entities.PreRestoreTable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.*;

@Setter
public class PreRestoreGui extends SimpleGui {

    private int page;

    private List<PreRestoreTable> preRestoreTableList;

    public PreRestoreGui(ServerPlayer player, int page, List<PreRestoreTable> preRestoreTables) {
        super(MenuType.GENERIC_9x6, player, false);

        this.preRestoreTableList = preRestoreTables;
        this.page = page;

        addButtons();
    }

    @Override
    public boolean canPlayerClose() {
        return true;
    }


    private void addButtons(){
        int firstIndex = this.page * 45;
        int tableSize = this.preRestoreTableList.size();
        int lastIndex = Math.min(firstIndex + 45, tableSize);


        for(int i=firstIndex; i<lastIndex; i++){
            int inventory_index = i-firstIndex;

            if(inventory_index>44) break;
            //System.out.println("Size: "+tableSize+" Ref: "+(tableSize-i));
            String inventory = this.preRestoreTableList.get(tableSize-i-1).getInventory();
            String armor = this.preRestoreTableList.get(tableSize-i-1).getArmor();
            String offHand = this.preRestoreTableList.get(tableSize-i-1).getOffHand();
            String enderChest = this.preRestoreTableList.get(tableSize-i-1).getEnderChest();
            String trinkets = this.preRestoreTableList.get(tableSize-i-1).getTrinkets();

            int xp = this.preRestoreTableList.get(tableSize-i-1).getXp();
            boolean isInventory = this.preRestoreTableList.get(tableSize-i-1).isTableType();

            this.setSlot(inventory_index, new GuiElementBuilder(isInventory ? Items.CHEST : Items.ENDER_CHEST)
                    .setName(Component.literal("Time: "+this.preRestoreTableList.get(tableSize-i-1).getDate()))
                    .addLoreLine(Component.literal("XpLevel: "+this.preRestoreTableList.get(tableSize-i-1).getXp()))
                    .setCallback(() -> {
                        Map<Integer, ItemStack> itemStackList = TableListGui.inventorySerialization(inventory, armor, offHand, player);

                        if(isInventory){
                            new InventoryGui(player, this.preRestoreTableList.getFirst().getName(), itemStackList, enderChest, trinkets, xp, this).open();
                        } else {
                            new EnderChestGui(player, this.preRestoreTableList.getFirst().getName(), enderChest, this).open();
                        }

                    })
                    .build());

        }

        if (lastIndex < this.preRestoreTableList.size()) {
            this.setSlot(53, new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal("Next Page"))
                    .setCallback(() -> new PreRestoreGui(player, page + 1, this.preRestoreTableList).open())
                    .build());
        }

        this.setSlot(49, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("Back to tables list"))
                .setCallback(() -> new TableListGui(player, preRestoreTableList.getFirst().getName()).open())
                .build());

        if (page > 0) {
            this.setSlot(45, new GuiElementBuilder(Items.ARROW)
                    .setName(Component.literal("Previous Page"))
                    .setCallback(() -> new PreRestoreGui(player, page - 1, this.preRestoreTableList).open())
                    .build());
        }
//        System.out.println(TableListGui.activeTables);
    }

}


