package sh.orbis.orbzones.api;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneTest {
    @Test
    void normalizesCornersAndUsesInclusiveBounds() {
        Zone zone = new Zone(
                UUID.randomUUID(),
                "spawn",
                "default",
                new BlockPosition(10, 20, 30),
                new BlockPosition(1, 2, 3),
                ZoneProtection.ALL
        );

        assertEquals(new BlockPosition(1, 2, 3), zone.min());
        assertEquals(new BlockPosition(10, 20, 30), zone.max());
        assertTrue(zone.contains("default", new BlockPosition(1, 2, 3)));
        assertTrue(zone.contains("default", new BlockPosition(10, 20, 30)));
        assertFalse(zone.contains("other", new BlockPosition(5, 5, 5)));
        assertFalse(zone.contains("default", new BlockPosition(11, 5, 5)));
    }

    @Test
    void floorsNegativeWorldCoordinates() {
        assertEquals(
                new BlockPosition(-2, 4, -1),
                BlockPosition.from(new Vector3d(-1.01, 4.99, -0.01))
        );
    }

    @Test
    void updatesOneProtectionFlagWithoutChangingOthers() {
        ZoneProtection updated = ZoneProtection.ALL.with(ZoneAction.PLACE, false);

        assertTrue(updated.protects(ZoneAction.BREAK));
        assertFalse(updated.protects(ZoneAction.PLACE));
        assertTrue(updated.protects(ZoneAction.DAMAGE));
    }

    @Test
    void usesEveryRegionButExposesAggregateBounds() {
        Zone zone = new Zone(
                UUID.randomUUID(),
                "multi",
                "default",
                List.of(
                        new ZoneRegion(
                                new BlockPosition(0, 5, 10),
                                new BlockPosition(2, 7, 12)
                        ),
                        new ZoneRegion(
                                new BlockPosition(20, 1, 30),
                                new BlockPosition(21, 3, 31)
                        )
                ),
                ZoneProtection.ALL
        );

        assertEquals(new BlockPosition(0, 1, 10), zone.min());
        assertEquals(new BlockPosition(21, 7, 31), zone.max());
        assertTrue(zone.contains("default", new BlockPosition(1, 6, 11)));
        assertTrue(zone.contains("default", new BlockPosition(20, 2, 30)));
        assertFalse(zone.contains("default", new BlockPosition(10, 4, 20)));
        assertEquals(39, zone.volume());
    }
}
