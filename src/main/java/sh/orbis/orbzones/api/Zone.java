package sh.orbis.orbzones.api;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Zone(
        @Nonnull UUID id,
        @Nonnull String name,
        @Nonnull String worldName,
        @Nonnull List<ZoneRegion> regions,
        @Nonnull ZoneProtection protection
) {
    public Zone {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(regions, "regions");
        Objects.requireNonNull(protection, "protection");
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A zone must contain at least one region");
        }

        regions = List.copyOf(regions);
        for (int current = 0; current < regions.size(); current++) {
            for (int other = current + 1; other < regions.size(); other++) {
                if (regions.get(current).overlaps(regions.get(other))) {
                    throw new IllegalArgumentException("Regions in the same zone cannot overlap");
                }
            }
        }
    }

    public Zone(
            @Nonnull UUID id,
            @Nonnull String name,
            @Nonnull String worldName,
            @Nonnull BlockPosition firstCorner,
            @Nonnull BlockPosition secondCorner,
            @Nonnull ZoneProtection protection
    ) {
        this(id, name, worldName, List.of(new ZoneRegion(firstCorner, secondCorner)), protection);
    }

    public boolean contains(@Nonnull String candidateWorld, @Nonnull BlockPosition position) {
        return worldName.equals(candidateWorld)
                && regions.stream().anyMatch(region -> region.contains(position));
    }

    @Nonnull
    public BlockPosition min() {
        return new BlockPosition(
                regions.stream().mapToInt(region -> region.min().x()).min().orElseThrow(),
                regions.stream().mapToInt(region -> region.min().y()).min().orElseThrow(),
                regions.stream().mapToInt(region -> region.min().z()).min().orElseThrow()
        );
    }

    @Nonnull
    public BlockPosition max() {
        return new BlockPosition(
                regions.stream().mapToInt(region -> region.max().x()).max().orElseThrow(),
                regions.stream().mapToInt(region -> region.max().y()).max().orElseThrow(),
                regions.stream().mapToInt(region -> region.max().z()).max().orElseThrow()
        );
    }

    @Nonnull
    public Zone withProtection(@Nonnull ZoneProtection updatedProtection) {
        return new Zone(id, name, worldName, regions, updatedProtection);
    }

    @Nonnull
    public Zone withRegion(@Nonnull ZoneRegion region) {
        if (regions.stream().anyMatch(existing -> existing.overlaps(region))) {
            throw new IllegalArgumentException("The new region overlaps an existing region in '" + name + "'");
        }

        List<ZoneRegion> updatedRegions = new ArrayList<>(regions);
        updatedRegions.add(region);
        return new Zone(id, name, worldName, updatedRegions, protection);
    }

    public long volume() {
        long volume = 0;
        for (ZoneRegion region : regions) {
            volume = Math.addExact(volume, region.volume());
        }
        return volume;
    }
}
