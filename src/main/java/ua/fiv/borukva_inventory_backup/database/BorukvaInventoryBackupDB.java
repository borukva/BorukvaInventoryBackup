package ua.fiv.borukva_inventory_backup.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ua.fiv.borukva_inventory_backup.config.ModConfigs;
import ua.fiv.borukva_inventory_backup.database.entities.*;

import java.sql.SQLException;
import java.util.List;

public class BorukvaInventoryBackupDB {
    private Dao<DeathTable, String> deathTableDao;
    private Dao<LoginTable, String> loginTableDao;
    private Dao<LogoutTable, String> logoutTableDao;
    private Dao<PreRestoreTable, String> preRestoreTableDao;
    private Dao<PendingTrinketsTable, Integer> pendingTrinketsDao;
    private final JdbcConnectionSource connectionSource;

    public BorukvaInventoryBackupDB() throws SQLException {
        connectionSource = new JdbcConnectionSource("jdbc:h2:./" + ModConfigs.DATABASE_NAME);
        init();
        addTrinketsColumns("VARCHAR(2000000)");

        modifyTableForH2();
    }

    public BorukvaInventoryBackupDB(String url, String user, String password) throws SQLException {
        connectionSource = new JdbcConnectionSource(url, user, password);
        init();
        addTrinketsColumns("LONGTEXT");
    }

    private void init() throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, DeathTable.class);
        TableUtils.createTableIfNotExists(connectionSource, LoginTable.class);
        TableUtils.createTableIfNotExists(connectionSource, LogoutTable.class);
        TableUtils.createTableIfNotExists(connectionSource, PreRestoreTable.class);
        TableUtils.createTableIfNotExists(connectionSource, PendingTrinketsTable.class);

        deathTableDao = DaoManager.createDao(connectionSource, DeathTable.class);
        loginTableDao = DaoManager.createDao(connectionSource, LoginTable.class);
        logoutTableDao = DaoManager.createDao(connectionSource, LogoutTable.class);
        preRestoreTableDao = DaoManager.createDao(connectionSource, PreRestoreTable.class);
        pendingTrinketsDao = DaoManager.createDao(connectionSource, PendingTrinketsTable.class);
    }

    /** Tables created before trinket support lack the column, and ORMLite never alters existing tables. */
    private void addTrinketsColumns(String columnType) throws SQLException {
        addTrinketsColumnIfMissing(deathTableDao, "death_table", columnType);
        addTrinketsColumnIfMissing(loginTableDao, "login_table", columnType);
        addTrinketsColumnIfMissing(logoutTableDao, "logout_table", columnType);
        addTrinketsColumnIfMissing(preRestoreTableDao, "pre_restore_table", columnType);
    }

    private static void addTrinketsColumnIfMissing(Dao<?, ?> dao, String table, String columnType) throws SQLException {
        try {
            dao.queryRaw("SELECT trinkets FROM " + table + " WHERE 1 = 0").close();
        } catch (Exception missingColumn) {
            dao.executeRawNoArgs("ALTER TABLE " + table + " ADD COLUMN trinkets " + columnType);
        }
    }

    public void addDataDeath(String name, String world, String place,
                             String date, String reason, String inventory, String armor, String offHand, String enderChest, int xp, String trinkets) throws SQLException {
        deleteOldestRecord(name, deathTableDao);
        DeathTable deathTable = new DeathTable(name, world, place, date, inventory, armor, offHand, enderChest, xp, trinkets, reason);
        deathTableDao.create(deathTable);
    }

    public void addDataLogin(String name, String world, String place,
                             String date, String inventory, String armor, String offHand, String enderChest, int xp, String trinkets) throws SQLException {
        deleteOldestRecord(name, loginTableDao);
        LoginTable loginTable = new LoginTable(name, world, place, date, inventory, armor, offHand, enderChest, xp, trinkets);
        loginTableDao.create(loginTable);
    }

    public void addDataLogout(String name, String world, String place,
                              String date, String inventory, String armor, String offHand, String enderChest, int xp, String trinkets) throws SQLException {
        deleteOldestRecord(name, logoutTableDao);
        LogoutTable logoutTable = new LogoutTable(name, world, place, date, inventory, armor, offHand, enderChest, xp, trinkets);
        logoutTableDao.create(logoutTable);
    }

    public void addDataPreRestore(String name, String date, String inventory, String armor, String offHand, String enderChest, boolean isInventory, int xp, String trinkets) throws SQLException {
        deleteOldestRecord(name, preRestoreTableDao);
        PreRestoreTable preRestoreTable = new PreRestoreTable(name, date, inventory, armor, offHand, enderChest, isInventory, xp, trinkets);
        preRestoreTableDao.create(preRestoreTable);
    }

    /** Replaces whatever was already waiting for this player: only the latest restore applies. */
    public void setPendingTrinkets(String name, String date, String trinkets) throws SQLException {
        pendingTrinketsDao.delete(pendingTrinketsDao.queryForEq("name", name));
        pendingTrinketsDao.create(new PendingTrinketsTable(name, date, trinkets));
    }

    public PendingTrinketsTable getPendingTrinkets(String name) throws SQLException {
        List<PendingTrinketsTable> results = pendingTrinketsDao.queryForEq("name", name);
        return results.isEmpty() ? null : results.getLast();
    }

    public void deletePendingTrinkets(int id) throws SQLException {
        pendingTrinketsDao.deleteById(id);
    }

    public List<DeathTable> getDeathData(String playerName) throws SQLException {
        return deathTableDao.queryForEq("name", playerName);
    }

    public List<LoginTable> getLoginData(String playerName) throws SQLException {
        return loginTableDao.queryForEq("name", playerName);
    }

    public List<LogoutTable> getLogoutData(String playerName) throws SQLException {
        return logoutTableDao.queryForEq("name", playerName);
    }

    public List<PreRestoreTable> getPreRestoreData(String playerName) throws SQLException {
        return preRestoreTableDao.queryForEq("name", playerName);
    }

    private <T extends BaseEntity> void deleteOldestRecord(String playerName, Dao<T, String> dao) throws SQLException {
        List<T> results = dao.queryForEq("name", playerName);

        if (results != null && !results.isEmpty()) {
            int maxRecords = ModConfigs.MAX_RECORDS;

            if (results.size() >= maxRecords && maxRecords > 0) {
                List<T> oldestRecords = dao.queryBuilder().orderBy("date", true).where().eq("name", playerName).query();

                // Make room for the record that the caller inserts immediately after this method.
                int recordsToDeleteCount = oldestRecords.size() - maxRecords + 1;
                if (recordsToDeleteCount > 0) {
                    List<T> recordsToDelete = oldestRecords.subList(0, recordsToDeleteCount);
                    dao.delete(recordsToDelete);
                }
            }
        }
    }

    public boolean playerLoginTableExist(String playerName) throws SQLException {
        List<LoginTable> results = loginTableDao.queryForEq("name", playerName);
        return results != null && !results.isEmpty();
    }

    private void modifyTableForH2() throws SQLException {
        String[] alterStatementsDeath = {
                "ALTER TABLE death_table ALTER COLUMN inventory VARCHAR(2000000);",
                "ALTER TABLE death_table ALTER COLUMN armor VARCHAR(1000000);",
                "ALTER TABLE death_table ALTER COLUMN offHand VARCHAR(300000);",
                "ALTER TABLE death_table ALTER COLUMN enderChest VARCHAR(2000000);",
                "ALTER TABLE death_table ALTER COLUMN trinkets VARCHAR(2000000);"
        };
        String[] alterStatementsLogin = {
                "ALTER TABLE login_table ALTER COLUMN inventory VARCHAR(2000000);",
                "ALTER TABLE login_table ALTER COLUMN armor VARCHAR(1000000);",
                "ALTER TABLE login_table ALTER COLUMN offHand VARCHAR(300000);",
                "ALTER TABLE login_table ALTER COLUMN enderChest VARCHAR(2000000);",
                "ALTER TABLE login_table ALTER COLUMN trinkets VARCHAR(2000000);"
        };
        String[] alterStatementsLogout = {
                "ALTER TABLE logout_table ALTER COLUMN inventory VARCHAR(2000000);",
                "ALTER TABLE logout_table ALTER COLUMN armor VARCHAR(1000000);",
                "ALTER TABLE logout_table ALTER COLUMN offHand VARCHAR(300000);",
                "ALTER TABLE logout_table ALTER COLUMN enderChest VARCHAR(2000000);",
                "ALTER TABLE logout_table ALTER COLUMN trinkets VARCHAR(2000000);"
        };
        String[] alterStatementsPreRestore = {
                "ALTER TABLE pre_restore_table ALTER COLUMN inventory VARCHAR(2000000);",
                "ALTER TABLE pre_restore_table ALTER COLUMN armor VARCHAR(1000000);",
                "ALTER TABLE pre_restore_table ALTER COLUMN offHand VARCHAR(300000);",
                "ALTER TABLE pre_restore_table ALTER COLUMN enderChest VARCHAR(2000000);",
                "ALTER TABLE pre_restore_table ALTER COLUMN trinkets VARCHAR(2000000);"
        };

        for (String sql : alterStatementsDeath) {
            deathTableDao.executeRawNoArgs(sql);
        }
        for (String sql : alterStatementsLogin) {
            loginTableDao.executeRawNoArgs(sql);
        }
        for (String sql : alterStatementsLogout) {
            logoutTableDao.executeRawNoArgs(sql);
        }
        for (String sql : alterStatementsPreRestore) {
            preRestoreTableDao.executeRawNoArgs(sql);
        }
    }

    public void closeDbConnection() throws Exception {
        if (connectionSource != null) {
            connectionSource.close();
        }
    }
}
