package ua.fiv.borukva_inventory_backup.actor;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.config.ModConfigs;
import ua.fiv.borukva_inventory_backup.database.BorukvaInventoryBackupDB;
import ua.fiv.borukva_inventory_backup.database.entities.DeathTable;
import ua.fiv.borukva_inventory_backup.database.entities.LoginTable;
import ua.fiv.borukva_inventory_backup.database.entities.LogoutTable;
import ua.fiv.borukva_inventory_backup.database.entities.PendingTrinketsTable;
import ua.fiv.borukva_inventory_backup.database.entities.PreRestoreTable;
import ua.fiv.borukva_inventory_backup.compat.TrinketsCompat;
import ua.fiv.borukva_inventory_backup.gui.*;
import ua.fiv.borukva_inventory_backup.util.TrinketEntry;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class DatabaseManagerActor extends AbstractBehavior<BActorMessages.Command> {

    @Getter
    private static BorukvaInventoryBackupDB borukvaInventoryBackupDB;

    private DatabaseManagerActor(ActorContext<BActorMessages.Command> context) {
        super(context);
    }

    public static Behavior<BActorMessages.Command> create() {
        return Behaviors.setup(DatabaseManagerActor::new);
    }

    @Override
    public Receive<BActorMessages.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(BActorMessages.InitializeDatabase.class, this::initializeDatabase)
                .onMessage(BActorMessages.SavePlayerDataOnPlayerDeath.class, this::onPlayerDeath)
                .onMessage(BActorMessages.SavePlayerDataOnPlayerConnect.class, this::onPlayerConnect)
                .onMessage(BActorMessages.SavePlayerDataOnPlayerLogout.class, this::onPlayerLogout)
                .onMessage(BActorMessages.SavePlayerDataOnPlayerRestore.class, this::onPlayerRestore)
                .onMessage(BActorMessages.SavePendingTrinkets.class, this::savePendingTrinkets)
                .onMessage(BActorMessages.ApplyPendingTrinkets.class, this::applyPendingTrinkets)
                .onMessage(BActorMessages.DeletePendingTrinkets.class, this::deletePendingTrinkets)
                .onMessage(BActorMessages.GetInventoryHistory.class, this::getInventoryHistory)
                .onMessage(BActorMessages.GetDeathTableMap.class, this::getDeathTableMap)
                .onMessage(BActorMessages.GetLogoutTableMap.class, this::getLogoutTableMap)
                .onMessage(BActorMessages.GetLoginTableMap.class, this::getLoginTableMap)
                .onMessage(BActorMessages.GetPreRestoreTableMap.class, this::getPreRestoreTableMap)
                .build();
    }

    private Behavior<BActorMessages.Command> initializeDatabase(BActorMessages.InitializeDatabase msg) {
        try {
            String dbType = ModConfigs.DATABASE_TYPE;

            if ("h2".equalsIgnoreCase(dbType)) {
                borukvaInventoryBackupDB = new BorukvaInventoryBackupDB();
                ModInit.LOGGER.info("Initialized H2 database.");
            } else {
                String url = ModConfigs.DB_URL;
                String user = ModConfigs.DB_USER;
                String password = ModConfigs.DB_PASSWORD;
                borukvaInventoryBackupDB = new BorukvaInventoryBackupDB(url, user, password);
                ModInit.LOGGER.info("Connecting to external database of type: {}", dbType);
            }

            ModInit.LOGGER.info("Database connection successfully established!");
        } catch (SQLException e) {
            ModInit.LOGGER.error("Failed to connect to the database!", e);
            throw new RuntimeException("Could not establish database connection", e);
        }
        return this;
    }


    private Behavior<BActorMessages.Command> onPlayerDeath(BActorMessages.SavePlayerDataOnPlayerDeath msg) {
        PlayerSnapshot snapshot = msg.snapshot();
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];

        try {
            borukvaInventoryBackupDB.addDataDeath(
                    snapshot.name(), snapshot.world(), snapshot.place(), formattedTime,
                    msg.deathReason(), snapshot.inventory(), snapshot.armor(), snapshot.offHand(),
                    snapshot.enderChest(), snapshot.xp(), snapshot.trinkets()
            );
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerConnect(BActorMessages.SavePlayerDataOnPlayerConnect msg) {
        PlayerSnapshot snapshot = msg.snapshot();
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];

        try {
            borukvaInventoryBackupDB.addDataLogin(
                    snapshot.name(), snapshot.world(), snapshot.place(), formattedTime,
                    snapshot.inventory(), snapshot.armor(), snapshot.offHand(),
                    snapshot.enderChest(), snapshot.xp(), snapshot.trinkets()
            );
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerLogout(BActorMessages.SavePlayerDataOnPlayerLogout msg) {
        PlayerSnapshot snapshot = msg.snapshot();
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];

        try {
            borukvaInventoryBackupDB.addDataLogout(
                    snapshot.name(), snapshot.world(), snapshot.place(), formattedTime,
                    snapshot.inventory(), snapshot.armor(), snapshot.offHand(),
                    snapshot.enderChest(), snapshot.xp(), snapshot.trinkets()
            );
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerRestore(BActorMessages.SavePlayerDataOnPlayerRestore msg) {
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        try {
            borukvaInventoryBackupDB.addDataPreRestore(msg.playerName(), formattedTime, msg.inventory(), msg.armor(), msg.offHand(), msg.enderChest(), msg.isInventory(), msg.xp(), msg.trinkets());
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> savePendingTrinkets(BActorMessages.SavePendingTrinkets msg) {
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        try {
            borukvaInventoryBackupDB.setPendingTrinkets(msg.playerName(), formattedTime, msg.trinkets());
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> applyPendingTrinkets(BActorMessages.ApplyPendingTrinkets msg) {
        PendingTrinketsTable pending;
        try {
            pending = borukvaInventoryBackupDB.getPendingTrinkets(msg.playerName());
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        if (pending == null) {
            return this;
        }

        // The record is deleted only once the trinkets are on the player, so a
        // player who leaves in between gets them on the next login instead.
        executeOnServer(msg.server(), () -> {
            ServerPlayer player = msg.server().getPlayerList().getPlayer(msg.playerName());
            if (player == null) {
                return;
            }
            List<TrinketEntry> entries = TrinketEntry.parse(pending.getTrinkets(), player.registryAccess());
            if (entries != null) {
                PlayerSnapshot snapshot = PlayerSnapshot.capture(player);
                ModInit.getDatabaseManagerActor().tell(new BActorMessages.SavePlayerDataOnPlayerRestore(
                        snapshot.name(), snapshot.inventory(), snapshot.armor(), snapshot.offHand(),
                        snapshot.enderChest(), true, snapshot.xp(), snapshot.trinkets()));
                TrinketsCompat.restore(player, entries);
                ModInit.LOGGER.info("Restored {} pending trinket(s) to {}", entries.size(), msg.playerName());
            }
            ModInit.getDatabaseManagerActor().tell(new BActorMessages.DeletePendingTrinkets(pending.getId()));
        });
        return this;
    }

    private Behavior<BActorMessages.Command> deletePendingTrinkets(BActorMessages.DeletePendingTrinkets msg) {
        try {
            borukvaInventoryBackupDB.deletePendingTrinkets(msg.id());
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getInventoryHistory(BActorMessages.GetInventoryHistory msg) {
        try {
            if (!borukvaInventoryBackupDB.playerLoginTableExist(msg.playerName())) {
                executeOnServer(msg.server(), () -> msg.player().sendSystemMessage(
                        Component.literal("There is no such player!")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                ));
                return this;
            }

            executeOnServer(msg.server(), () -> new TableListGui(msg.player(), msg.playerName()).open());
        } catch (Exception e) {
            ModInit.LOGGER.warn("Error processing getInventoryHistory command: {}", e.getMessage());
            if (e instanceof SQLException) {
                throw new SQLExceptionWrapper((SQLException) e);
            }
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getDeathTableMap(BActorMessages.GetDeathTableMap msg) {
        try {
            List<DeathTable> deathTableList = borukvaInventoryBackupDB.getDeathData(msg.playerName());
            executeOnServer(msg.server(), () -> {
                if (deathTableList == null || deathTableList.isEmpty()) {
                    msg.player().sendSystemMessage(Component.literal("There are no records for this player in the death database."));
                } else {
                    new DeathHistoryGui(msg.player(), 0, deathTableList).open();
                }
            });
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching death table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getLogoutTableMap(BActorMessages.GetLogoutTableMap msg) {
        try {
            List<LogoutTable> logoutTableList = borukvaInventoryBackupDB.getLogoutData(msg.playerName());
            executeOnServer(msg.server(), () -> {
                if (logoutTableList == null || logoutTableList.isEmpty()) {
                    msg.player().sendSystemMessage(Component.literal("There are no records for this player in the logout database."));
                } else {
                    new LogoutHistoryGui(msg.player(), 0, logoutTableList).open();
                }
            });
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching logout table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getLoginTableMap(BActorMessages.GetLoginTableMap msg) {
        try {
            List<LoginTable> loginTableList = borukvaInventoryBackupDB.getLoginData(msg.playerName());
            executeOnServer(msg.server(), () -> {
                if (loginTableList == null || loginTableList.isEmpty()) {
                    msg.player().sendSystemMessage(Component.literal("There are no records for this player in the login database."));
                } else {
                    new LoginHistoryGui(msg.player(), 0, loginTableList).open();
                }
            });
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching login table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getPreRestoreTableMap(BActorMessages.GetPreRestoreTableMap msg) {
        try {
            List<PreRestoreTable> preRestoreTableList = borukvaInventoryBackupDB.getPreRestoreData(msg.playerName());
            executeOnServer(msg.server(), () -> {
                if (preRestoreTableList == null || preRestoreTableList.isEmpty()) {
                    msg.player().sendSystemMessage(Component.literal("There are no records for this player in the pre-restore database."));
                } else {
                    new PreRestoreGui(msg.player(), 0, preRestoreTableList).open();
                }
            });
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching pre-restore table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private static void executeOnServer(MinecraftServer server, Runnable action) {
        server.execute(action);
    }
}
