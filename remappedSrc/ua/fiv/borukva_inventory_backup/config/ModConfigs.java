package ua.fiv.borukva_inventory_backup.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import ua.fiv.borukva_inventory_backup.ModInit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfigs {
    @Getter
    private static ModConfig config;

    public static int MAX_RECORDS;
    public static String DATABASE_TYPE;
    public static String DATABASE_NAME;
    public static String DB_URL;
    public static String DB_USER;
    public static String DB_PASSWORD;

    public static void registerConfigs() {
        loadConfig();
        populateStaticFields();
    }

    private static void loadConfig() {
        File configFile = new File("config/" + ModInit.MOD_ID + ".json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                ModInit.LOGGER.error("Could not read config file", e);
                config = new ModConfig();             }
        } else {
            config = new ModConfig();
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                ModInit.LOGGER.error("Could not create or write to config file", e);
            }
        }
    }

    private static void populateStaticFields() {
        if (config != null) {
            MAX_RECORDS = config.maxRecords;
            DATABASE_TYPE = config.databaseType;
            DATABASE_NAME = config.databaseName;
            DB_URL = config.databaseUrl;
            DB_USER = config.databaseUser;
            DB_PASSWORD = config.databasePassword;
        }
    }
}