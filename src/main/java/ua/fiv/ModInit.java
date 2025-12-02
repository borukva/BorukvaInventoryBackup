package ua.fiv;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import ua.fiv.actor.BActorMessages;
import ua.fiv.actor.DatabaseManagerSupervisor;
import ua.fiv.commands.GetInventoryHistoryCommand;
import ua.fiv.config.ModConfigs;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModInit implements ModInitializer {
	public static final String MOD_ID = "borukva_inventory_backup";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@Getter
	private static ActorSystem actorSystem;
	@Getter
	private static ActorRef databaseManagerActor;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);

		GetInventoryHistoryCommand.registerCommandOfflinePlayer();
		ModConfigs.registerConfigs();

		actorSystem = ActorSystem.create("BorukvaInventoryBackupActorSystem");
		databaseManagerActor = actorSystem.actorOf(DatabaseManagerSupervisor.props(), "databaseManagerSupervisor");
	}

	private void onServerStarting(MinecraftServer server) {
		ModInit.getDatabaseManagerActor().tell(
				new BActorMessages.InitializeDatabase(server), ActorRef.noSender());
	}

}