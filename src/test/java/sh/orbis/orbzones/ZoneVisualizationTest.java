package sh.orbis.orbzones;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneProtection;
import sh.orbis.orbzones.api.ZoneRegion;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZoneVisualizationTest {
    @Test
    void createsAnExactTimedTranslucentBoxForTheInclusiveBounds() {
        Zone zone = new Zone(
                UUID.randomUUID(),
                "building",
                "default",
                new BlockPosition(1, 2, 3),
                new BlockPosition(4, 6, 8),
                ZoneProtection.ALL
        );

        DisplayDebug packet = ZoneVisualization.createPackets(zone).getFirst();
        Matrix4f transform = new Matrix4f().set(packet.matrix);

        assertEquals(DebugShape.Cube, packet.shape);
        assertEquals(new Vector3f(3.0f, 4.5f, 6.0f), transform.getTranslation(new Vector3f()));
        assertEquals(new Vector3f(4.0f, 5.0f, 6.0f), transform.getScale(new Vector3f()));
        assertEquals(new Vector3f(0.0f, 0.8f, 0.2f), packet.color);
        assertEquals(ZoneVisualization.DISPLAY_SECONDS, packet.time);
        assertEquals((byte) DebugUtils.FLAG_NONE, packet.flags);
        assertEquals(0.2f, packet.opacity);
        assertNull(packet.frustumProjection);
    }

    @Test
    void createsOneVisualizationForEveryRegion() {
        Zone zone = new Zone(
                UUID.randomUUID(),
                "building",
                "default",
                List.of(
                        new ZoneRegion(
                                new BlockPosition(1, 2, 3),
                                new BlockPosition(4, 6, 8)
                        ),
                        new ZoneRegion(
                                new BlockPosition(10, 20, 30),
                                new BlockPosition(11, 21, 31)
                        )
                ),
                ZoneProtection.ALL
        );

        assertEquals(2, ZoneVisualization.createPackets(zone).size());
    }

    @Test
    void keepsSelectionVisualizationUntilExplicitlyCleared() {
        ZoneRegion region = new ZoneRegion(
                new BlockPosition(1, 2, 3),
                new BlockPosition(4, 6, 8)
        );

        DisplayDebug packet = ZoneVisualization.createSelectionPacket(region);

        assertEquals(Float.MAX_VALUE, packet.time);
    }
}
