package ua.fiv.actor;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public interface BActorMessages {
    interface Command {}

    record SavePlayerDataOnPlayerDeath(ServerPlayerEntity player, DamageSource source) implements Command {}
    record SavePlayerDataOnPlayerConnect(ServerPlayerEntity player) implements Command {}
    record SavePlayerDataOnPlayerLogout(ServerPlayerEntity player) implements Command {}
    record SavePlayerDataOnPlayerRestore(String playerName, String inventory, String armor, String offHand, String enderChest, boolean isInventory, int xp) implements Command {}
    record SavePlayerDataOnPlayerRestoreNbt(String playerName, NbtList inventory, NbtList enderChest, boolean isInventory, int xp) implements Command {}
    record GetDeathTableMap(ServerPlayerEntity player, String playerName) implements Command {}
    record GetLogoutTableMap(ServerPlayerEntity player, String playerName) implements Command {}
    record GetLoginTableMap(ServerPlayerEntity player, String playerName) implements Command {}
    record GetPreRestoreTableMap(ServerPlayerEntity player, String playerName) implements Command {}
    record GetInventoryHistory(CommandContext<ServerCommandSource> context) implements Command {}
    record InitializeDatabase(MinecraftServer server) implements Command {}
}