package ua.fiv.borukva_inventory_backup.actor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import ua.fiv.borukva_inventory_backup.compat.TrinketsCompat;
import ua.fiv.borukva_inventory_backup.gui.InventoryGui;

import java.util.List;

/** Immutable player state captured by a Minecraft-server-thread callback. */
public record PlayerSnapshot(
        String name,
        String world,
        String place,
        String inventory,
        String armor,
        String offHand,
        String enderChest,
        int xp,
        String trinkets
) {
    public static PlayerSnapshot capture(ServerPlayer player) {
        List<ItemStack> armor = List.of(
                player.getInventory().getItem(36),
                player.getInventory().getItem(37),
                player.getInventory().getItem(38),
                player.getInventory().getItem(39)
        );

        return new PlayerSnapshot(
                player.getName().getString(),
                player.level().dimension().identifier().toString(),
                "%.2f %.2f %.2f".formatted(player.getX(), player.getY(), player.getZ()),
                InventoryGui.playerItems(player.getInventory().getNonEquipmentItems(), player).toString(),
                InventoryGui.playerItems(armor, player).toString(),
                InventoryGui.playerItems(List.of(player.getOffhandItem()), player).toString(),
                InventoryGui.playerItems(player.getEnderChestInventory().getItems(), player).toString(),
                player.experienceLevel,
                TrinketsCompat.capture(player)
        );
    }
}
