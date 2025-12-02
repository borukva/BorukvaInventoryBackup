package ua.fiv;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
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
	private static ActorSystem<BActorMessages.Command> actorSystem;
	@Getter
	private static ActorRef<BActorMessages.Command> databaseManagerActor;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);

		GetInventoryHistoryCommand.registerCommandOfflinePlayer();
		ModConfigs.registerConfigs();

		actorSystem = ActorSystem.create(DatabaseManagerSupervisor.create(), "BorukvaInventoryBackupActorSystem");
		databaseManagerActor = actorSystem;
	}

	private void onServerStarting(MinecraftServer server) {
		databaseManagerActor.tell(new BActorMessages.InitializeDatabase(server));
	}
}