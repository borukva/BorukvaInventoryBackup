package ua.fiv.actor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.gui.SimpleGui;
import lombok.Getter;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.AbstractBehavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.Receive;
import ua.fiv.ModInit;
import ua.fiv.config.ModConfigs;
import ua.fiv.data_base.BorukvaInventoryBackupDB;
import ua.fiv.data_base.entities.DeathTable;
import ua.fiv.data_base.entities.LoginTable;
import ua.fiv.data_base.entities.LogoutTable;
import ua.fiv.data_base.entities.PreRestoreTable;
import ua.fiv.gui.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
                .onMessage(BActorMessages.SavePlayerDataOnPlayerRestoreNbt.class, this::onPlayerRestoreNbt)
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
        ServerPlayerEntity player = msg.player();
        DamageSource source = msg.source();

        DefaultedList<ItemStack> inventory = player.getInventory().getMainStacks();
        List<ItemStack> armor = List.of(
                player.getInventory().getStack(36),
                player.getInventory().getStack(37),
                player.getInventory().getStack(38),
                player.getInventory().getStack(39)
        );
        DefaultedList<ItemStack> enderChest = player.getEnderChestInventory().heldStacks;
        List<ItemStack> offHand = List.of(player.getOffHandStack());

        String name = player.getName().getString();
        String world = player.getWorld().getRegistryKey().getValue().toString();
        String place = "%.2f %.2f %.2f".formatted(player.getX(), player.getY(), player.getZ());
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        String deathReason = source.getName();
        String inventr = InventoryGui.playerItems(inventory, player).toString();
        String armorString = InventoryGui.playerItems(armor, player).toString();
        String offHandString = InventoryGui.playerItems(offHand, player).toString();
        String enderChestString = InventoryGui.playerItems(enderChest, player).toString();
        int xp = player.experienceLevel;

        try {
            borukvaInventoryBackupDB.addDataDeath(name, world, place, formattedTime, deathReason, inventr, armorString, offHandString, enderChestString, xp);
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerConnect(BActorMessages.SavePlayerDataOnPlayerConnect msg) {
        ServerPlayerEntity player = msg.player();
        DefaultedList<ItemStack> inventory = player.getInventory().getMainStacks();
        List<ItemStack> armor = List.of(
                player.getInventory().getStack(36),
                player.getInventory().getStack(37),
                player.getInventory().getStack(38),
                player.getInventory().getStack(39)
        );
        DefaultedList<ItemStack> enderChest = player.getEnderChestInventory().heldStacks;
        List<ItemStack> offHand = List.of(player.getOffHandStack());

        String name = player.getName().getString();
        String world = player.getWorld().getRegistryKey().getValue().toString();
        String place = "%.2f %.2f %.2f".formatted(player.getX(), player.getY(), player.getZ());
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        String inventr = InventoryGui.playerItems(inventory, player).toString();
        String armorString = InventoryGui.playerItems(armor, player).toString();
        String offHandString = InventoryGui.playerItems(offHand, player).toString();
        String enderChestString = InventoryGui.playerItems(enderChest, player).toString();
        int xp = player.experienceLevel;

        try {
            borukvaInventoryBackupDB.addDataLogin(name, world, place, formattedTime, inventr, armorString, offHandString, enderChestString, xp);
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerLogout(BActorMessages.SavePlayerDataOnPlayerLogout msg) {
        ServerPlayerEntity player = msg.player();
        DefaultedList<ItemStack> inventory = player.getInventory().getMainStacks();
        List<ItemStack> armor = List.of(
                player.getInventory().getStack(36),
                player.getInventory().getStack(37),
                player.getInventory().getStack(38),
                player.getInventory().getStack(39)
        );
        DefaultedList<ItemStack> enderChest = player.getEnderChestInventory().heldStacks;
        List<ItemStack> offHand = List.of(player.getOffHandStack());

        String name = player.getName().getString();
        String world = player.getWorld().getRegistryKey().getValue().toString();
        String place = "%.2f %.2f %.2f".formatted(player.getX(), player.getY(), player.getZ());
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        String inventr = InventoryGui.playerItems(inventory, player).toString();
        String armorString = InventoryGui.playerItems(armor, player).toString();
        String offHandString = InventoryGui.playerItems(offHand, player).toString();
        String enderChestString = InventoryGui.playerItems(enderChest, player).toString();
        int xp = player.experienceLevel;

        try {
            borukvaInventoryBackupDB.addDataLogout(name, world, place, formattedTime, inventr, armorString, offHandString, enderChestString, xp);
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerRestore(BActorMessages.SavePlayerDataOnPlayerRestore msg) {
        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        try {
            borukvaInventoryBackupDB.addDataPreRestore(msg.playerName(), formattedTime, msg.inventory(), msg.armor(), msg.offHand(), msg.enderChest(), msg.isInventory(), msg.xp());
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> onPlayerRestoreNbt(BActorMessages.SavePlayerDataOnPlayerRestoreNbt msg) {
        NbtList mainInventory = new NbtList();
        NbtList armor = new NbtList();
        NbtList offHand = new NbtList();

        for (NbtElement nbtElement : msg.inventory()) {
            NbtCompound itemNbt = (NbtCompound) nbtElement;
            Optional<Byte> slotOpt = itemNbt.getByte("Slot");

            if (slotOpt.isPresent()) {
                byte slot = slotOpt.get();
                if (slot >= (byte) 100) {
                    armor.add(itemNbt);
                } else if (slot >= (byte) -106) {
                    offHand.add(itemNbt);
                } else {
                    mainInventory.add(itemNbt);
                }
            }
        }

        String formattedTime = LocalDateTime.now().toString().replace("T", " ").split("\\.")[0];
        try {
            borukvaInventoryBackupDB.addDataPreRestore(
                    msg.playerName(), formattedTime, mainInventory.toString(),
                    armor.toString(), offHand.toString(), msg.enderChest().toString(),
                    msg.isInventory(), msg.xp()
            );
        } catch (SQLException e) {
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getInventoryHistory(BActorMessages.GetInventoryHistory msg) {
        CommandContext<ServerCommandSource> context = msg.context();
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            String playerName = StringArgumentType.getString(context, "player");

            if (!borukvaInventoryBackupDB.playerLoginTableExist(playerName)) {
                context.getSource().sendFeedback(() -> Text.literal("There is no such player!").formatted(Formatting.RED, Formatting.BOLD), false);
                return this;
            }

            SimpleGui tableListGui = new TableListGui(player, playerName);
            tableListGui.open();
        } catch (Exception e) { // Catching broader exception for command context issues
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
            if (deathTableList == null || deathTableList.isEmpty()) {
                msg.player().sendMessage(Text.literal("There are no records for this player in the death database."));
            } else {
                new DeathHistoryGui(msg.player(), 0, deathTableList).open();
            }
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching death table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getLogoutTableMap(BActorMessages.GetLogoutTableMap msg) {
        try {
            List<LogoutTable> logoutTableList = borukvaInventoryBackupDB.getLogoutData(msg.playerName());
            if (logoutTableList == null || logoutTableList.isEmpty()) {
                msg.player().sendMessage(Text.literal("There are no records for this player in the logout database."));
            } else {
                new LogoutHistoryGui(msg.player(), 0, logoutTableList).open();
            }
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching logout table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getLoginTableMap(BActorMessages.GetLoginTableMap msg) {
        try {
            List<LoginTable> loginTableList = borukvaInventoryBackupDB.getLoginData(msg.playerName());
            if (loginTableList == null || loginTableList.isEmpty()) {
                msg.player().sendMessage(Text.literal("There are no records for this player in the login database."));
            } else {
                new LoginHistoryGui(msg.player(), 0, loginTableList).open();
            }
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching login table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }

    private Behavior<BActorMessages.Command> getPreRestoreTableMap(BActorMessages.GetPreRestoreTableMap msg) {
        try {
            List<PreRestoreTable> preRestoreTableList = borukvaInventoryBackupDB.getPreRestoreData(msg.playerName());
            if (preRestoreTableList == null || preRestoreTableList.isEmpty()) {
                msg.player().sendMessage(Text.literal("There are no records for this player in the pre-restore database."));
            } else {
                new PreRestoreGui(msg.player(), 0, preRestoreTableList).open();
            }
        } catch (SQLException e) {
            ModInit.LOGGER.error("Error fetching pre-restore table map:", e);
            throw new SQLExceptionWrapper(e);
        }
        return this;
    }
}