package ua.fiv.borukva_inventory_backup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.util.jar.JarInputStream;
import java.util.jar.JarFile;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModResourcesTest {
    @Test
    void metadataTargetsThe26_2RuntimeAndKeepsTheEntrypoint() throws Exception {
        Path metadataPath = FabricLoader.getInstance()
                .getModContainer("borukva_inventory_backup")
                .orElseThrow()
                .findPath("fabric.mod.json")
                .orElseThrow();
        JsonObject metadata;
        try (var reader = Files.newBufferedReader(metadataPath, StandardCharsets.UTF_8)) {
            metadata = JsonParser.parseReader(reader).getAsJsonObject();
        }

        assertEquals("borukva_inventory_backup", metadata.get("id").getAsString());
        assertEquals("0.4.0+26.2", metadata.get("version").getAsString());
        assertEquals("*", metadata.get("environment").getAsString());
        assertEquals(
                "ua.fiv.borukva_inventory_backup.ModInit",
                metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").get(0).getAsString()
        );

        JsonObject dependencies = metadata.getAsJsonObject("depends");
        assertEquals(">=0.19.3", dependencies.get("fabricloader").getAsString());
        assertEquals("~26.2", dependencies.get("minecraft").getAsString());
        assertEquals(">=25", dependencies.get("java").getAsString());
        assertNotNull(getClass().getResource("/assets/borukva_inventory_backup/icon.png"));
    }

    @Test
    void allSnapshotLifecycleMixinsRemainPackaged() throws Exception {
        JsonObject mixinConfig = readJson("/borukva_inventory_backup.mixins.json");
        assertEquals("JAVA_25", mixinConfig.get("compatibilityLevel").getAsString());

        JsonArray mixins = mixinConfig.getAsJsonArray("mixins");
        Set<String> names = StreamSupport.stream(mixins.spliterator(), false)
                .map(element -> element.getAsString())
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "OnPlayerDeathMixin",
                "OnPlayerLoginMixin",
                "OnPlayerLogoutMixin",
                "OnServerShutDownMixin"
        ), names);

        for (String mixin : names) {
            assertNotNull(getClass().getResource(
                    "/ua/fiv/borukva_inventory_backup/mixin/" + mixin + ".class"
            ));
        }
    }

    @Test
    void bundlesTheOfficialMySqlConnectorJArtifact() throws Exception {
        Path modJar = Path.of(System.getProperty("borukva.test.modJar"));
        assertTrue(Files.isRegularFile(modJar), modJar.toString());

        try (JarFile jar = new JarFile(modJar.toFile())) {
            var mysqlEntry = jar.stream()
                    .filter(entry -> entry.getName().startsWith("META-INF/jars/mysql-connector-j-8.0.33"))
                    .findFirst()
                    .orElseThrow();

            byte[] nestedJar = jar.getInputStream(mysqlEntry).readAllBytes();
            boolean driverPackaged = false;
            try (JarInputStream nested = new JarInputStream(new ByteArrayInputStream(nestedJar))) {
                for (var entry = nested.getNextJarEntry(); entry != null; entry = nested.getNextJarEntry()) {
                    if (entry.getName().equals("com/mysql/cj/jdbc/Driver.class")) {
                        driverPackaged = true;
                        break;
                    }
                }
            }
            assertTrue(driverPackaged, "MySQL driver class is missing from the nested connector jar");
            assertTrue(jar.stream().noneMatch(entry ->
                    entry.getName().contains("mysql-connector-java")
            ));
        }
    }

    private static JsonObject readJson(String resource) throws Exception {
        try (InputStream stream = ModResourcesTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }
}
