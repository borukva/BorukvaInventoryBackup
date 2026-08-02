package ua.fiv.borukva_inventory_backup.gui;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserNameToIdResolver;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.actor.BActorMessages;
import ua.fiv.borukva_inventory_backup.actor.PlayerSnapshot;
import ua.fiv.borukva_inventory_backup.util.OfflineInventorySnapshot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.*;

public class InventoryGui extends SimpleGui {

    public InventoryGui(ServerPlayer player, String playerName, Map<Integer, ItemStack> itemStackMap, String enderChest, int xp, SimpleGui caller) {
        super(MenuType.GENERIC_9x6, player, false);
        addItems(itemStackMap, enderChest, xp, playerName,caller);
    }


    private void addItems(Map<Integer, ItemStack> itemStackMap, String enderChest, int xp, String playerName, SimpleGui caller){
        int i = 0;
        for(ItemStack item: itemStackMap.values()){
            this.setSlot(i, new GuiElementBuilder(item)
                    .setCount(item.getCount())
                    .build());
            i++;
        }

        this.setSlot(53, new GuiElementBuilder(Items.PAPER)
                .setName(Component.literal("Backup player inventory(recovery will be irreversible)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .setCallback(() -> {
                    UUID uuid = getOfflinePlayerProfile(playerName, player.level().getServer());

                    if(this.player.level().getServer().getPlayerList().getPlayer(playerName) != null){
                        backUpPlayerItems(itemStackMap, xp, this.player.level().getServer().getPlayerList().getPlayer(playerName));
                        this.getPlayer().sendSystemMessage(Component.literal("You have successfully restored items to an online player!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                    } else {
                        saveOfflinePlayerInventory(uuid, xp,itemStackMap, playerName);
                        this.getPlayer().sendSystemMessage(Component.literal("You have successfully restored items to an offline player!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                    }

                })
                .build());

        this.setSlot(51, new GuiElementBuilder(Items.SHULKER_BOX)
                .setName(Component.literal("Backup player items to the box").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .setLore(new ArrayList<>(List.of(Component.literal("clear your inventory before issuing").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))))
                .setCallback(() -> {
                    backUpPlayerItemsToChest(itemStackMap, playerName, this.player);
                    this.getPlayer().sendSystemMessage(Component.literal("You have successfully restored items to box!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                })
                .build());

        this.setSlot(47, new GuiElementBuilder(Items.ENDER_CHEST)
                .setName(Component.literal("Player ender chest").withStyle(ChatFormatting.DARK_PURPLE))
                .setCallback(() -> new EnderChestGui(player, playerName, enderChest, this).open())
                .build());

        this.setSlot(46, new GuiElementBuilder(Items.EXPERIENCE_BOTTLE)
                .setName(Component.literal("XP level: "+xp).withStyle(ChatFormatting.YELLOW))
                .build());

        this.setSlot(45, new GuiElementBuilder(Items.EMERALD)
                .setName(Component.literal("Return back"))
                .setCallback(() -> caller.open())
                .build());
    }

    protected static void savePreRestorePlayerInventory(String playerName, String inventory, String armor, String offHand, String enderChest, boolean isInventory,int xp){
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.SavePlayerDataOnPlayerRestore(playerName, inventory, armor, offHand, enderChest, isInventory, xp));

    }

    public static UUID getOfflinePlayerProfile(String playerName, MinecraftServer server) {
        if (server == null) return null;

        UserNameToIdResolver cache = server.services().nameToIdCache();

        if (cache == null) return null;

        Optional<NameAndId> optionalGameProfile = cache.get(playerName);

        if (optionalGameProfile.isPresent()){
            NameAndId gameProfile = optionalGameProfile.get();
            return gameProfile.id();
        }
        return null;
    }

    private void backUpPlayerItems(Map<Integer, ItemStack> itemStackMap, int xp,ServerPlayer player){

        Inventory playerInventory = player.getInventory();

        PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
        savePreRestorePlayerInventory(snapshot.name(),
                snapshot.inventory(),
                snapshot.armor(),
                snapshot.offHand(),
                snapshot.enderChest(),
                true,
                snapshot.xp()
                );

        playerInventory.clearContent();

        for (int index = 0; index < 41; index++) {
            ItemStack itemStack = itemStackMap.getOrDefault(index, ItemStack.EMPTY);
            if(index < 4){
                playerInventory.setItem(36+index, itemStack);
            } else if (index==4) {
                playerInventory.setItem(40, itemStack);
            } else {
                playerInventory.setItem(index-5, itemStack);
            }
        }
        player.setExperienceLevels(xp);

    }

    private void saveOfflinePlayerInventory(UUID uuid, int xp, Map<Integer, ItemStack> itemStackMap, String playerName) {
        File playerDataDir = this.player.level().getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();

        try {
            File file2 = new File(playerDataDir, uuid.toString() + ".dat");

            CompoundTag nbtCompound = NbtIo.readCompressed(file2.toPath(), NbtAccounter.unlimitedHeap());

            CompoundTag currentEquipment = nbtCompound.getCompound("equipment").orElseGet(CompoundTag::new);
            ListTag inventoryList = nbtCompound.getList("Inventory").orElseGet(ListTag::new);
            ListTag enderChestLists = nbtCompound.getList("EnderItems").orElseGet(ListTag::new);

            OfflineInventorySnapshot snapshot = OfflineInventorySnapshot.fromPlayerData(
                    inventoryList, currentEquipment
            );
            savePreRestorePlayerInventory(
                    playerName,
                    snapshot.inventory(),
                    snapshot.armor(),
                    snapshot.offHand(),
                    OfflineInventorySnapshot.normalizeSlottedContainer(enderChestLists, 27),
                    true,
                    nbtCompound.getIntOr("XpLevel", 0)
            );

            inventoryList.clear();

            String[] slotNames = {"feet", "legs", "chest", "head", "offhand"};
            CompoundTag equipment = new CompoundTag();

            for (int index = 0; index < 5; index++) {
                ItemStack itemStack = itemStackMap.getOrDefault(index, ItemStack.EMPTY);
                if (!itemStack.isEmpty()) {
                    CompoundTag nbt = getItemStackNbt(itemStack, player.registryAccess().createSerializationContext(NbtOps.INSTANCE));
                    equipment.put(slotNames[index], nbt);
                }
            }

            for (int index = 5; index < 41; index++) {
                ItemStack itemStack = itemStackMap.getOrDefault(index, ItemStack.EMPTY);
                if (itemStack.isEmpty()) {
                    continue;
                }
                CompoundTag nbt = getItemStackNbt(itemStack, player.registryAccess().createSerializationContext(NbtOps.INSTANCE));
                nbt.putByte("Slot", (byte) (index - 5));
                inventoryList.add(nbt);
            }

            nbtCompound.put("Inventory", inventoryList);
            nbtCompound.put("equipment", equipment);
            nbtCompound.putInt("XpLevel", xp);

            NbtIo.writeCompressed(nbtCompound, file2.toPath());
        } catch (Exception e) {
            ModInit.LOGGER.error("Error when try save items to offline player: {}", e.getMessage());
        }

    }

    public static CompoundTag getItemStackNbt(ItemStack stack, DynamicOps<Tag> ops) {
        DataResult<Tag> result = ItemStack.CODEC.encodeStart(ops, stack);

        result.ifError(e -> {});

        Tag nbtElement = result.result().orElseGet(()->{
            CompoundTag plugNbt = new CompoundTag();
            plugNbt.put("components", new CompoundTag());
            plugNbt.putInt("count", 0);
            plugNbt.putString("id", "minecraft:air");
            return plugNbt;
        });

        CompoundTag nbtCompound = nbtElement.asCompound().orElseGet(()->{
            CompoundTag plugNbt = new CompoundTag();
            plugNbt.put("components", new CompoundTag());
            plugNbt.putInt("count", 0);
            plugNbt.putString("id", "minecraft:air");
            return plugNbt;
        });

        nbtCompound.putInt("count", stack.getCount());
        nbtCompound.putString("id", stack.getItem().toString());

        return nbtCompound;
    }

    public static ArrayList<String> playerItems(List<ItemStack> inventory, Player player){

        ArrayList<String> playerItems = new ArrayList<>();

        for(ItemStack itemStack: inventory){
            CompoundTag nbt = getItemStackNbt(itemStack, player.registryAccess().createSerializationContext(NbtOps.INSTANCE));
            playerItems.add(nbt.toString());
        }

        return playerItems;

    }

    public static void backUpPlayerItemsToChest(Map<Integer, ItemStack> itemStackMap, String playerName, ServerPlayer operatorPlayer){
        List<Integer> toRemove = new ArrayList<>();

        itemStackMap.forEach((index, item) -> {
            if (item.getItem() == Items.AIR) {
                toRemove.add(index);
            }
        });

        toRemove.forEach(itemStackMap::remove);

        List<ItemStack> list = itemStackMap.values().stream().toList();

        ItemStack chest;

        if(itemStackMap.size() > 27){

            chest = createChestItem(list.subList(27, list.size()), playerName+" second inventory", operatorPlayer);

            operatorPlayer.drop(chest, false, true);
        }

        chest = createChestItem(list.subList(0, Math.min(27, list.size())), playerName+" first inventory", operatorPlayer);

        operatorPlayer.drop(chest, false, true);
    }

    public static ItemStack createChestItem(List<ItemStack> items, String name, ServerPlayer operatorPlayer) {
        ListTag containerList = new ListTag();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                CompoundTag nbt = getItemStackNbt(stack, operatorPlayer.registryAccess().createSerializationContext(NbtOps.INSTANCE));
                slotTag.put("item", nbt);
                slotTag.putInt("slot", i); // chest has slots from 0 to 26
                containerList.add(slotTag);
            }
        }

        CompoundTag components = new CompoundTag();
        components.put("minecraft:container", containerList);
        components.putString("minecraft:item_name", name);

        CompoundTag blockEntityTag = new CompoundTag();
        blockEntityTag.put("components", components);
        blockEntityTag.putInt("count", 1);
        blockEntityTag.putString("id", "minecraft:chest");

        return ItemStack.CODEC.parse(operatorPlayer.registryAccess().createSerializationContext(NbtOps.INSTANCE), blockEntityTag).getOrThrow();
    }

}
