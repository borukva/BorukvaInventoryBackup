package ua.fiv.borukva_inventory_backup.actor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public interface BActorMessages {
    interface Command {}

    record SavePlayerDataOnPlayerDeath(PlayerSnapshot snapshot, String deathReason) implements Command {}
    record SavePlayerDataOnPlayerConnect(PlayerSnapshot snapshot) implements Command {}
    record SavePlayerDataOnPlayerLogout(PlayerSnapshot snapshot) implements Command {}
    record SavePlayerDataOnPlayerRestore(String playerName, String inventory, String armor, String offHand, String enderChest, boolean isInventory, int xp, String trinkets) implements Command {}
    record SavePendingTrinkets(String playerName, String trinkets) implements Command {}
    record ApplyPendingTrinkets(MinecraftServer server, String playerName) implements Command {}
    record DeletePendingTrinkets(int id) implements Command {}
    record GetDeathTableMap(MinecraftServer server, ServerPlayer player, String playerName) implements Command {}
    record GetLogoutTableMap(MinecraftServer server, ServerPlayer player, String playerName) implements Command {}
    record GetLoginTableMap(MinecraftServer server, ServerPlayer player, String playerName) implements Command {}
    record GetPreRestoreTableMap(MinecraftServer server, ServerPlayer player, String playerName) implements Command {}
    record GetInventoryHistory(MinecraftServer server, ServerPlayer player, String playerName) implements Command {}
    record InitializeDatabase(MinecraftServer server) implements Command {}
}
