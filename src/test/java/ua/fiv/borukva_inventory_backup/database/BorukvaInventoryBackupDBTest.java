package ua.fiv.borukva_inventory_backup.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.fiv.borukva_inventory_backup.config.ModConfigs;
import ua.fiv.borukva_inventory_backup.database.entities.BaseEntity;
import ua.fiv.borukva_inventory_backup.database.entities.DeathTable;
import ua.fiv.borukva_inventory_backup.database.entities.LoginTable;
import ua.fiv.borukva_inventory_backup.database.entities.LogoutTable;
import ua.fiv.borukva_inventory_backup.database.entities.PendingTrinketsTable;
import ua.fiv.borukva_inventory_backup.database.entities.PreRestoreTable;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Path;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorukvaInventoryBackupDBTest {
    private BorukvaInventoryBackupDB database;
    private int previousMaxRecords;
    private String previousDatabaseName;

    @BeforeEach
    void openDatabase() throws Exception {
        previousMaxRecords = ModConfigs.MAX_RECORDS;
        previousDatabaseName = ModConfigs.DATABASE_NAME;

        Path databaseDirectory = Path.of("build", "test-databases");
        Files.createDirectories(databaseDirectory);
        ModConfigs.DATABASE_NAME = databaseDirectory.resolve(UUID.randomUUID().toString())
                .toString()
                .replace('\\', '/');
        ModConfigs.MAX_RECORDS = 100;
        database = new BorukvaInventoryBackupDB();
    }

    @AfterEach
    void closeDatabase() throws Exception {
        if (database != null) {
            database.closeDbConnection();
        }
        ModConfigs.MAX_RECORDS = previousMaxRecords;
        ModConfigs.DATABASE_NAME = previousDatabaseName;
    }

    @Test
    void persistsAndSeparatesEverySnapshotCategory() throws Exception {
        database.addDataDeath(
                "Alex", "minecraft:overworld", "1 64 2", "2026-08-02T12:00:00Z",
                "minecraft:fall", "death-inventory", "death-armor", "death-offhand",
                "death-ender", 17, "death-trinkets"
        );
        database.addDataLogin(
                "Alex", "minecraft:the_nether", "3 70 4", "2026-08-02T12:01:00Z",
                "login-inventory", "login-armor", "login-offhand", "login-ender", 18, "login-trinkets"
        );
        database.addDataLogout(
                "Alex", "minecraft:the_end", "5 80 6", "2026-08-02T12:02:00Z",
                "logout-inventory", "logout-armor", "logout-offhand", "logout-ender", 19, "logout-trinkets"
        );
        database.addDataPreRestore(
                "Alex", "2026-08-02T12:03:00Z", "restore-inventory", "restore-armor",
                "restore-offhand", "restore-ender", true, 20, "restore-trinkets"
        );
        database.addDataLogin(
                "Steve", "minecraft:overworld", "0 64 0", "2026-08-02T12:04:00Z",
                "other-inventory", "other-armor", "other-offhand", "other-ender", 1, null
        );

        DeathTable death = database.getDeathData("Alex").getFirst();
        assertSnapshot(death, "Alex", "2026-08-02T12:00:00Z", "death", 17);
        assertEquals("minecraft:overworld", death.getWorld());
        assertEquals("1 64 2", death.getPlace());
        assertEquals("minecraft:fall", death.getReason());

        LoginTable login = database.getLoginData("Alex").getFirst();
        assertSnapshot(login, "Alex", "2026-08-02T12:01:00Z", "login", 18);
        assertEquals("minecraft:the_nether", login.getWorld());
        assertEquals("3 70 4", login.getPlace());

        LogoutTable logout = database.getLogoutData("Alex").getFirst();
        assertSnapshot(logout, "Alex", "2026-08-02T12:02:00Z", "logout", 19);
        assertEquals("minecraft:the_end", logout.getWorld());
        assertEquals("5 80 6", logout.getPlace());

        PreRestoreTable preRestore = database.getPreRestoreData("Alex").getFirst();
        assertSnapshot(preRestore, "Alex", "2026-08-02T12:03:00Z", "restore", 20);
        assertTrue(preRestore.isTableType());

        assertEquals(1, database.getLoginData("Alex").size());
        assertEquals(1, database.getLoginData("Steve").size());
        assertTrue(database.playerLoginTableExist("Alex"));
        assertFalse(database.playerLoginTableExist("Unknown"));
    }

    @Test
    void retentionLimitIncludesTheRecordBeingInserted() throws Exception {
        ModConfigs.MAX_RECORDS = 2;

        for (int index = 0; index < 4; index++) {
            database.addDataLogin(
                    "Alex", "minecraft:overworld", "0 64 0",
                    "2026-08-02T12:0" + index + ":00Z",
                    "inventory-" + index, "armor", "offhand", "ender", index, null
            );
        }

        var records = database.getLoginData("Alex");
        assertEquals(2, records.size());
        assertEquals(
                Set.of("2026-08-02T12:02:00Z", "2026-08-02T12:03:00Z"),
                records.stream().map(LoginTable::getDate).collect(Collectors.toSet())
        );
    }

    @Test
    void addsTheTrinketsColumnToTablesFromOlderVersions() throws Exception {
        database.closeDbConnection();

        Path databaseDirectory = Path.of("build", "test-databases");
        ModConfigs.DATABASE_NAME = databaseDirectory.resolve(UUID.randomUUID().toString())
                .toString()
                .replace('\\', '/');
        try (Connection connection = DriverManager.getConnection("jdbc:h2:./" + ModConfigs.DATABASE_NAME);
             Statement statement = connection.createStatement()) {
            // Layout written by 0.4.0, before trinket support.
            statement.execute("CREATE TABLE login_table (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), " +
                    "date VARCHAR(255), inventory VARCHAR(2000000), armor VARCHAR(1000000), offHand VARCHAR(300000), " +
                    "enderChest VARCHAR(2000000), xp INT, world VARCHAR(255), place VARCHAR(255))");
            statement.execute("INSERT INTO login_table (name, date, inventory, armor, offHand, enderChest, xp, world, place) " +
                    "VALUES ('Alex', '2026-08-02 12:00:00', 'inv', 'armor', 'offhand', 'ender', 3, 'minecraft:overworld', '0 64 0')");
        }

        database = new BorukvaInventoryBackupDB();
        LoginTable old = database.getLoginData("Alex").getFirst();
        assertEquals("inv", old.getInventory());
        assertNull(old.getTrinkets());

        database.addDataLogin("Alex", "minecraft:overworld", "0 64 0", "2026-08-02 12:01:00",
                "inv", "armor", "offhand", "ender", 4, "[]");
        assertEquals(2, database.getLoginData("Alex").size());

        // Opening an already migrated database must not fail.
        database.closeDbConnection();
        database = new BorukvaInventoryBackupDB();
        assertEquals(2, database.getLoginData("Alex").size());
    }

    @Test
    void keepsOnlyTheLatestPendingTrinketsPerPlayer() throws Exception {
        assertNull(database.getPendingTrinkets("Alex"));

        database.setPendingTrinkets("Alex", "2026-08-02 12:00:00", "first");
        database.setPendingTrinkets("Alex", "2026-08-02 12:01:00", "second");
        database.setPendingTrinkets("Steve", "2026-08-02 12:02:00", "other");

        PendingTrinketsTable pending = database.getPendingTrinkets("Alex");
        assertEquals("second", pending.getTrinkets());

        database.deletePendingTrinkets(pending.getId());
        assertNull(database.getPendingTrinkets("Alex"));
        assertEquals("other", database.getPendingTrinkets("Steve").getTrinkets());
    }

    private static void assertSnapshot(BaseEntity snapshot, String player, String date,
                                       String valuePrefix, int experience) {
        assertEquals(player, snapshot.getName());
        assertEquals(date, snapshot.getDate());
        assertEquals(valuePrefix + "-inventory", snapshot.getInventory());
        assertEquals(valuePrefix + "-armor", snapshot.getArmor());
        assertEquals(valuePrefix + "-offhand", snapshot.getOffHand());
        assertEquals(valuePrefix + "-ender", snapshot.getEnderChest());
        assertEquals(experience, snapshot.getXp());
        assertEquals(valuePrefix + "-trinkets", snapshot.getTrinkets());
    }
}
