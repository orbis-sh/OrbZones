package sh.orbis.orbzones.api;

import org.joml.Vector3dc;
import org.joml.Vector3ic;

import javax.annotation.Nonnull;

public record BlockPosition(int x, int y, int z) {
    @Nonnull
    public static BlockPosition from(@Nonnull Vector3dc position) {
        return new BlockPosition(
                (int) Math.floor(position.x()),
                (int) Math.floor(position.y()),
                (int) Math.floor(position.z())
        );
    }

    @Nonnull
    public static BlockPosition from(@Nonnull Vector3ic position) {
        return new BlockPosition(position.x(), position.y(), position.z());
    }
}
