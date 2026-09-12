package sh.orbis.orbzones;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneAction;
import sh.orbis.orbzones.api.ZoneRegion;
import sh.orbis.orbzones.storage.ZoneStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneManagerPersistenceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsCreatesUpdatesAndDeletes() throws IOException {
        Path dataFile = temporaryDirectory.resolve("zones.json");
        ZoneManager manager = manager(dataFile);
        Zone created = manager.create(
                "Spawn",
                "default",
                new BlockPosition(8, 9, 10),
                new BlockPosition(1, 2, 3)
        );
        manager.setProtection("spawn", ZoneAction.PLACE, false);

        ZoneManager reloaded = manager(dataFile);
        Zone stored = reloaded.getByName("SPAWN").orElseThrow();
        assertEquals(created.id(), stored.id());
        assertFalse(stored.protection().placeBlocks());
        assertTrue(stored.protection().breakBlocks());
        assertTrue(Files.readString(dataFile).contains("\"version\": 2"));

        assertTrue(reloaded.delete("spawn"));
        assertFalse(reloaded.delete("spawn"));
        assertTrue(manager(dataFile).getZones().isEmpty());
    }

    @Test
    void addsRegionsToTheSameNameIgnoringCase() throws IOException {
        ZoneManager manager = manager(temporaryDirectory.resolve("zones.json"));
        Zone original = manager.create(
                "Spawn",
                "default",
                new BlockPosition(0, 0, 0),
                new BlockPosition(1, 1, 1)
        );
        Zone updated = manager.create(
                "spawn",
                "default",
                new BlockPosition(3, 3, 3),
                new BlockPosition(4, 4, 4)
        );

        assertEquals(original.id(), updated.id());
        assertEquals(2, updated.regions().size());
        assertTrue(updated.contains("default", new BlockPosition(0, 0, 0)));
        assertTrue(updated.contains("default", new BlockPosition(4, 4, 4)));
        assertFalse(updated.contains("default", new BlockPosition(2, 2, 2)));
        assertEquals(16, updated.volume());
    }

    @Test
    void rejectsOverlappingRegionsInTheSameZone() throws IOException {
        ZoneManager manager = manager(temporaryDirectory.resolve("zones.json"));
        manager.create(
                "Spawn",
                "default",
                new BlockPosition(0, 0, 0),
                new BlockPosition(5, 5, 5)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.create(
                        "spawn",
                        "default",
                        new BlockPosition(5, 5, 5),
                        new BlockPosition(10, 10, 10)
                )
        );
    }

    @Test
    void loadsVersionOneZonesAsTheirFirstRegion() throws IOException {
        Path dataFile = temporaryDirectory.resolve("zones.json");
        Files.writeString(
                dataFile,
                """
                        {
                          "version": 1,
                          "zones": [{
                            "id": "d13cb9ef-3bf1-4f1d-bf9e-bcaf0351af9a",
                            "name": "legacy",
                            "world": "default",
                            "min": {"x": 1, "y": 2, "z": 3},
                            "max": {"x": 4, "y": 5, "z": 6},
                            "protection": {
                              "breakBlocks": true,
                              "placeBlocks": true,
                              "damageBlocks": true
                            }
                          }]
                        }
                        """
        );

        ZoneManager manager = manager(dataFile);
        Zone legacy = manager.getByName("legacy").orElseThrow();

        assertEquals(
                List.of(new ZoneRegion(
                        new BlockPosition(1, 2, 3),
                        new BlockPosition(4, 5, 6)
                )),
                legacy.regions()
        );
        manager.setProtection("legacy", ZoneAction.PLACE, false);
        assertTrue(Files.readString(dataFile).contains("\"version\": 2"));
    }

    @Test
    void rejectsIncompleteJsonInsteadOfSilentlyDisablingProtection() throws IOException {
        Path dataFile = temporaryDirectory.resolve("zones.json");
        Files.writeString(
                dataFile,
                """
                        {
                          "version": 1,
                          "zones": [{
                            "id": "d13cb9ef-3bf1-4f1d-bf9e-bcaf0351af9a",
                            "name": "broken",
                            "world": "default",
                            "min": {"x": 0, "y": 0, "z": 0},
                            "max": {"x": 1, "y": 1, "z": 1},
                            "protection": {"breakBlocks": true}
                          }]
                        }
                        """
        );

        assertThrows(IOException.class, () -> manager(dataFile));
    }

    private static ZoneManager manager(Path dataFile) throws IOException {
        return new ZoneManager(
                new ZoneStorage(dataFile),
                (transition, event, exception) -> {
                    throw new AssertionError("Unexpected listener failure", exception);
                }
        );
    }
}
