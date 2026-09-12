package sh.orbis.orbzones;

import com.hypixel.hytale.math.matrix.Matrix4dUtil;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.joml.Matrix4d;
import org.joml.Vector3f;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneRegion;

import javax.annotation.Nonnull;
import java.util.List;

public final class ZoneVisualization {
    public static final float DISPLAY_SECONDS = 30.0f;
    static final float SELECTION_DISPLAY_SECONDS = Float.MAX_VALUE;

    private static final Vector3f COLOR = new Vector3f(0.0f, 0.8f, 0.2f);
    private static final float OPACITY = 0.2f;

    private ZoneVisualization() {
    }

    @Nonnull
    public static List<DisplayDebug> createPackets(@Nonnull Zone zone) {
        return zone.regions().stream().map(ZoneVisualization::createPacket).toList();
    }

    @Nonnull
    public static DisplayDebug createPacket(@Nonnull ZoneRegion region) {
        return createPacket(region, DISPLAY_SECONDS);
    }

    @Nonnull
    static DisplayDebug createSelectionPacket(@Nonnull ZoneRegion region) {
        return createPacket(region, SELECTION_DISPLAY_SECONDS);
    }

    @Nonnull
    private static DisplayDebug createPacket(@Nonnull ZoneRegion region, float displaySeconds) {
        double sizeX = (double) region.max().x() - region.min().x() + 1.0;
        double sizeY = (double) region.max().y() - region.min().y() + 1.0;
        double sizeZ = (double) region.max().z() - region.min().z() + 1.0;
        double centerX = region.min().x() + sizeX / 2.0;
        double centerY = region.min().y() + sizeY / 2.0;
        double centerZ = region.min().z() + sizeZ / 2.0;

        Matrix4d transform = new Matrix4d()
                .translation(centerX, centerY, centerZ)
                .scale(sizeX, sizeY, sizeZ);

        return new DisplayDebug(
                DebugShape.Cube,
                Matrix4dUtil.asFloatData(transform),
                COLOR,
                displaySeconds,
                (byte) DebugUtils.FLAG_NONE,
                null,
                OPACITY
        );
    }

    public static void showSelection(@Nonnull PlayerRef playerRef, @Nonnull ZoneRegion region) {
        clear(playerRef);
        playerRef.getPacketHandler().write(createSelectionPacket(region));
    }

    public static void clear(@Nonnull PlayerRef playerRef) {
        playerRef.getPacketHandler().write(new ClearDebugShapes());
    }
}
