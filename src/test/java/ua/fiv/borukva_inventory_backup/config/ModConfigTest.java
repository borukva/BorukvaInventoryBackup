package ua.fiv.borukva_inventory_backup.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void defaultsMatchTheGeneratedConfigurationContract() {
        ModConfig config = new ModConfig();

        assertEquals(100, config.maxRecords);
        assertEquals("H2", config.databaseType);
        assertEquals("borukva_inventory_backup", config.databaseName);
        assertEquals("localhost:3306", config.databaseUrl);
        assertEquals("user", config.databaseUser);
        assertEquals("password", config.databasePassword);
    }

    @Test
    void jsonUsesTheExistingExternalFieldNames() {
        JsonObject json = JsonParser.parseString(GSON.toJson(new ModConfig())).getAsJsonObject();

        assertEquals(100, json.get("max_records").getAsInt());
        assertEquals("H2", json.get("database_type").getAsString());
        assertEquals("borukva_inventory_backup", json.get("database_name").getAsString());
        assertTrue(json.has("c_database"));
        assertEquals("localhost:3306", json.get("database_url").getAsString());
        assertEquals("user", json.get("database_user").getAsString());
        assertEquals("password", json.get("database_password").getAsString());
        assertFalse(json.has("maxRecords"));
    }

    @Test
    void existingJsonOverridesDefaults() {
        ModConfig config = GSON.fromJson("""
                {
                  "max_records": 25,
                  "database_type": "MySQL",
                  "database_name": "inventory_history",
                  "database_url": "db.example:3306",
                  "database_user": "backup",
                  "database_password": "secret"
                }
                """, ModConfig.class);

        assertEquals(25, config.maxRecords);
        assertEquals("MySQL", config.databaseType);
        assertEquals("inventory_history", config.databaseName);
        assertEquals("db.example:3306", config.databaseUrl);
        assertEquals("backup", config.databaseUser);
        assertEquals("secret", config.databasePassword);
    }
}
