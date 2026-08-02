package ua.fiv.borukva_inventory_backup.gui;

import com.mojang.serialization.DataResult;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.commands.GetInventoryHistoryCommand;
import ua.fiv.borukva_inventory_backup.util.InventorySerializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TableListGui extends SimpleGui {


    public TableListGui(ServerPlayer player, String playerName) {
        super(MenuType.GENERIC_9x1, player, false);
        this.setTitle(Component.literal(playerName+"'s tables list"));

        addButtons(playerName);
    }

    private void addButtons(String playerName){
        this.setSlot(2, new GuiElementBuilder(Items.CHEST)
                .setName(Component.literal("Login history").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .setCallback(() -> GetInventoryHistoryCommand.getLoginTableMap(player, playerName))

                .build());

        this.setSlot(3, new GuiElementBuilder(Items.CHEST)
                .setName(Component.literal("Logout history").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .setCallback(() -> GetInventoryHistoryCommand.getLogoutTableMap(player, playerName))

                .build());

        this.setSlot(5, new GuiElementBuilder(Items.CHEST)
                .setName(Component.literal("Death history").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .setCallback(() -> GetInventoryHistoryCommand.getDeathTableMap(player, playerName))

                .build());

        this.setSlot(6, new GuiElementBuilder(Items.CHEST)
                .setName(Component.literal("Backups history").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
                .setCallback(() -> GetInventoryHistoryCommand.getPreRestoreTableMap(player, playerName))

                .build());
    }

    protected static Map<Integer, ItemStack> inventorySerialization(String inventory, String armor, String offHand, ServerPlayer player){
        Map<Integer, ItemStack> itemsToGive = new HashMap<>();
        Level world = player.level();

        CompoundTag nbtCompoundArmor = InventorySerializer.deserializeInventory(armor);
        //System.out.println("armor: "+armor);
        ListTag nbtListArmor = nbtCompoundArmor.getList("Inventory").get();
        //System.out.println("NbtArmor "+ nbtListArmor.toString());

        int index = 0;
        for(Tag nbtElement: nbtListArmor){
            CompoundTag itemNbt = (CompoundTag) nbtElement;

            //System.out.println("SlotByte: "+itemNbt.getByte("Slot"));
            ItemStack itemStack;
            //System.out.println("BLOCKTAG: "+itemNbt.getString("id")); //
            if(!itemNbt.getString("id").get().equals("minecraft:air")){

                DataResult<ItemStack> result =
                        ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), itemNbt);

                itemStack = result.result().orElseGet(() -> new ItemStack(Items.AIR));

            } else {
                itemStack = new ItemStack(Items.AIR);
            }

            itemsToGive.put(index, itemStack);
            index++;
        }

        CompoundTag nbtCompoundOffHand = InventorySerializer.deserializeInventory(offHand);
       // System.out.println("OffHand: "+nbtCompoundOffHand);
        ListTag nbtListOffHand = nbtCompoundOffHand.getList("Inventory").get();


        for(Tag nbtElement: nbtListOffHand){
            CompoundTag itemNbt = (CompoundTag) nbtElement;

            //System.out.println("SlotByte: "+itemNbt.getByte("Slot"));
            //System.out.println(itemNbt);

            ItemStack itemStack;

            //System.out.println("BLOCKTAG: "+itemNbt.getString("id")); //
            if(!itemNbt.getString("id").get().equals("minecraft:air")){

                DataResult<ItemStack> result =
                        ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), itemNbt);

                itemStack = result.result().orElseGet(() -> new ItemStack(Items.AIR));

            } else {
                itemStack = new ItemStack(Items.AIR);
            }

            itemsToGive.put(index, itemStack);
            index++;
        }


        CompoundTag nbtCompoundInventory = InventorySerializer.deserializeInventory(inventory);
        ListTag nbtListInventory = nbtCompoundInventory.getList("Inventory").get();
       // System.out.println("inv: "+inventory);

        for(Tag nbtElement: nbtListInventory){
            CompoundTag itemNbt = (CompoundTag)nbtElement;

            //System.out.println("SlotByte: "+itemNbt.getByte("Slot"));
            //System.out.println(itemNbt);
            ItemStack itemStack;

            //System.out.println("BLOCKTAG: "+itemNbt.getString("id")); //
            if(!itemNbt.getString("id").get().equals("minecraft:air")){
                DataResult<ItemStack> result =
                        ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), itemNbt);

                itemStack = result.result().orElseGet(() -> new ItemStack(Items.AIR));

            } else {
                itemStack = new ItemStack(Items.AIR);
            }

            itemsToGive.put(index, itemStack);
            index++;
        }

        return itemsToGive;
    }

    protected static Map<Integer, ItemStack> inventorySerialization(String enderChest, ServerPlayer player) {
        try {
            Level world = player.level();

            CompoundTag nbtCompound = InventorySerializer.deserializeInventory(enderChest);
            ListTag inventoryList = nbtCompound
                    .getList("Inventory")
                    .orElseThrow(() -> new IllegalArgumentException("No Inventory tag found"));

            AtomicInteger index = new AtomicInteger(0);

            return inventoryList.stream()
                    .map(el -> el.asCompound().orElse(null))
                    .filter(Objects::nonNull)
                    .map(itemNbt -> {
                        String id = getStringOr(itemNbt, "id", "minecraft:air");

                        ItemStack stack;

                        if (!id.equals("minecraft:air")) {
                            DataResult<ItemStack> result =
                                    ItemStack.CODEC.parse(world.registryAccess().createSerializationContext(NbtOps.INSTANCE), itemNbt);

                            stack = result.result().orElseGet(() -> new ItemStack(Items.AIR));
                        } else {
                            stack = new ItemStack(Items.AIR);
                        }

                        return Map.entry(index.getAndIncrement(), stack);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        } catch (Exception e) {
            ModInit.LOGGER.error("Exception when try to serialize ender chest: "+e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static String getStringOr(CompoundTag nbt, String key, String def) {
        return nbt.getString(key).orElse(def);
    }
}
