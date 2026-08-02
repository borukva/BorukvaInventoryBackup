package ua.fiv.borukva_inventory_backup.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.util.OfflineInventorySnapshot;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.*;

import static ua.fiv.borukva_inventory_backup.gui.InventoryGui.playerItems;

public class EnderChestGui extends SimpleGui {



    public EnderChestGui(ServerPlayer player, String playerName,String enderChest, SimpleGui caller) {
        super(MenuType.GENERIC_9x4, player, false);

        Map<Integer, ItemStack> enderChestMap = TableListGui.inventorySerialization(enderChest, player);

        addItems(enderChestMap, playerName,caller);
    }

    private void addItems(Map<Integer, ItemStack> enderChestMap, String playerName,SimpleGui caller){
        int i = 0;
        for(ItemStack item: enderChestMap.values()){
            this.setSlot(i, new GuiElementBuilder(item)
                    .setCount(item.getCount())
                    .build());
            i++;
        }

        this.setSlot(33, new GuiElementBuilder(Items.SHULKER_BOX)
                .setName(Component.literal("Backup player items to the box").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .setLore(new ArrayList<>(List.of(Component.literal("clear your inventory before issuing").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))))
                .setCallback(() -> {
                    InventoryGui.backUpPlayerItemsToChest(enderChestMap, playerName, this.player);
                    this.getPlayer().sendSystemMessage(Component.literal("You have successfully restored items to box!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                })
                .build());

        this.setSlot(35, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("Backup player ender chest(recovery will be irreversible)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .setCallback(() -> {
                    UUID uuid = InventoryGui.getOfflinePlayerProfile(playerName, player.level().getServer());

                    if(this.player.level().getServer().getPlayerList().getPlayer(playerName) != null){
                        backUpPlayerItems(enderChestMap, this.player.level().getServer().getPlayerList().getPlayer(playerName));
                        this.getPlayer().sendSystemMessage(Component.literal("You have successfully restored items to an online player!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                    } else {
                        saveOfflinePlayerEnderChest(uuid, enderChestMap, playerName);
                        this.getPlayer().sendSystemMessage(Component.literal("You have successfully restored items to an offline player!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                    }

                })
                .build());

        this.setSlot(27, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("Return back"))
                .setCallback(() -> caller.open())
                .build());
    }

    private void backUpPlayerItems(Map<Integer, ItemStack> itemStackMap, ServerPlayer player){
        PlayerEnderChestContainer enderChestInventory = player.getEnderChestInventory();

        InventoryGui.savePreRestorePlayerInventory(player.getName().getString(),
                "[]",
                "[]",
                "[]",
                playerItems(player.getEnderChestInventory().getItems(), player).toString(),
                false,
                0
        );

        enderChestInventory.clearContent();
        for (int index = 0; index < 27; index++) {
            ItemStack itemStack = itemStackMap.getOrDefault(index, ItemStack.EMPTY);
            enderChestInventory.setItem(index, itemStack);
        }
    }

    private void saveOfflinePlayerEnderChest(UUID uuid, Map<Integer, ItemStack> itemStackMap, String playerName) {
        File playerDataDir = this.player.level().getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();

//        System.out.println(playerDataDir);
        try {
            File file2 = new File(playerDataDir, uuid.toString() + ".dat");

            CompoundTag nbtCompound = NbtIo.readCompressed(file2.toPath(), NbtAccounter.unlimitedHeap());

            ListTag inventoryList = nbtCompound.getList("EnderItems").orElseGet(ListTag::new);


            InventoryGui.savePreRestorePlayerInventory(playerName,
                    "[]",
                    "[]",
                    "[]",
                    OfflineInventorySnapshot.normalizeSlottedContainer(inventoryList, 27),
                    false,
                    0
            );


            inventoryList.clear();

            for (int index = 0; index < 27; index++) {
                ItemStack itemStack = itemStackMap.getOrDefault(index, ItemStack.EMPTY);
                if (itemStack.isEmpty()) {
                    continue;
                }
                CompoundTag nbt = InventoryGui.getItemStackNbt(itemStack, player.registryAccess().createSerializationContext(NbtOps.INSTANCE));

                nbt.putByte("Slot", (byte) index);

                inventoryList.add(nbt);
            }
            //System.out.print("OffPlayer: "+inventoryList);

            nbtCompound.put("EnderItems", inventoryList);

            NbtIo.writeCompressed(nbtCompound, file2.toPath());
        } catch (Exception e) {
            ModInit.LOGGER.warn(e.getMessage());
        }

    }

}
