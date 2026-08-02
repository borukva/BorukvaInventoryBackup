package ua.fiv.borukva_inventory_backup.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.actor.BActorMessages;

public class GetInventoryHistoryCommand {

    public static void registerCommandOfflinePlayer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("binvbackup")
                        .requires(Permissions.require("borukva.rollback", 4))
                        .then(Commands
                                .argument("player", StringArgumentType.string())
                                .suggests(PLAYER_NAME_SUGGESTIONS)
                                .executes(GetInventoryHistoryCommand::getInventoryHistory))));
    }

    public static int getInventoryHistory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.GetInventoryHistory(
                        context.getSource().getServer(), player,
                        StringArgumentType.getString(context, "player")
                ));
        return 1;
    }

    public static void getDeathTableMap(ServerPlayer player, String playerName) {
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.GetDeathTableMap(player.level().getServer(), player, playerName));
    }

    public static void getLogoutTableMap(ServerPlayer player, String playerName) {
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.GetLogoutTableMap(player.level().getServer(), player, playerName));
    }

    public static void getLoginTableMap(ServerPlayer player, String playerName) {
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.GetLoginTableMap(player.level().getServer(), player, playerName));
    }

    public static void getPreRestoreTableMap(ServerPlayer player, String playerName) {
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.GetPreRestoreTableMap(player.level().getServer(), player, playerName));
    }

    public static final SuggestionProvider<CommandSourceStack> PLAYER_NAME_SUGGESTIONS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();

        // Suggest online player names
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            builder.suggest(player.getName().getString());
        }

        return builder.buildFuture();
    };
}
