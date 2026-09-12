package sh.orbis.orbzones.api;

import javax.annotation.Nonnull;
import java.util.Objects;

public record ZoneRegion(@Nonnull BlockPosition min, @Nonnull BlockPosition max) {
    public ZoneRegion {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");

        BlockPosition normalizedMin = new BlockPosition(
                Math.min(min.x(), max.x()),
                Math.min(min.y(), max.y()),
                Math.min(min.z(), max.z())
        );
        BlockPosition normalizedMax = new BlockPosition(
                Math.max(min.x(), max.x()),
                Math.max(min.y(), max.y()),
                Math.max(min.z(), max.z())
        );
        min = normalizedMin;
        max = normalizedMax;
    }

    public boolean contains(@Nonnull BlockPosition position) {
        return position.x() >= min.x() && position.x() <= max.x()
                && position.y() >= min.y() && position.y() <= max.y()
                && position.z() >= min.z() && position.z() <= max.z();
    }

    public boolean overlaps(@Nonnull ZoneRegion other) {
        return min.x() <= other.max.x() && max.x() >= other.min.x()
                && min.y() <= other.max.y() && max.y() >= other.min.y()
                && min.z() <= other.max.z() && max.z() >= other.min.z();
    }

    public long volume() {
        long width = (long) max.x() - min.x() + 1;
        long height = (long) max.y() - min.y() + 1;
        long depth = (long) max.z() - min.z() + 1;
        return Math.multiplyExact(Math.multiplyExact(width, height), depth);
    }
}
