package sh.orbis.orbzones.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneProtection;
import sh.orbis.orbzones.api.ZoneRegion;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ZoneStorage {
    private static final int FORMAT_VERSION = 2;
    private static final int LEGACY_FORMAT_VERSION = 1;

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ZoneStorage(@Nonnull Path file) {
        this.file = file;
    }

    @Nonnull
    public List<Zone> load() throws IOException {
        if (Files.notExists(file)) {
            return List.of();
        }

        Document document;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            document = gson.fromJson(reader, Document.class);
        } catch (JsonParseException exception) {
            throw new IOException("Invalid zone JSON in " + file, exception);
        }

        if (document == null
                || (document.version != LEGACY_FORMAT_VERSION && document.version != FORMAT_VERSION)
                || document.zones == null) {
            throw new IOException("Unsupported or incomplete zone data in " + file);
        }

        List<Zone> zones = new ArrayList<>(document.zones.size());
        for (ZoneData data : document.zones) {
            zones.add(data.toZone(file, document.version));
        }
        return List.copyOf(zones);
    }

    public void save(@Nonnull List<Zone> zones) throws IOException {
        Path directory = file.getParent();
        if (directory == null) {
            throw new IOException("Zone storage path has no parent directory: " + file);
        }
        Files.createDirectories(directory);

        Path temporaryFile = Files.createTempFile(directory, "zones-", ".tmp");
        try {
            Document document = Document.from(zones);
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                gson.toJson(document, writer);
            }

            try {
                Files.move(
                        temporaryFile,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static final class Document {
        private int version;
        private List<ZoneData> zones;

        private Document() {
        }

        private Document(int version, List<ZoneData> zones) {
            this.version = version;
            this.zones = zones;
        }

        private static Document from(List<Zone> zones) {
            return new Document(FORMAT_VERSION, zones.stream().map(ZoneData::from).toList());
        }
    }

    private static final class ZoneData {
        private String id;
        private String name;
        private String world;
        private PositionData min;
        private PositionData max;
        private List<RegionData> regions;
        private ProtectionData protection;

        private ZoneData() {
        }

        private ZoneData(
                String id,
                String name,
                String world,
                List<RegionData> regions,
                ProtectionData protection
        ) {
            this.id = id;
            this.name = name;
            this.world = world;
            this.regions = regions;
            this.protection = protection;
        }

        private static ZoneData from(Zone zone) {
            return new ZoneData(
                    zone.id().toString(),
                    zone.name(),
                    zone.worldName(),
                    zone.regions().stream().map(RegionData::from).toList(),
                    ProtectionData.from(zone.protection())
            );
        }

        private Zone toZone(Path source, int formatVersion) throws IOException {
            if (id == null
                    || name == null
                    || world == null
                    || protection == null
                    || !protection.isComplete()) {
                throw new IOException("Incomplete zone entry in " + source);
            }

            try {
                List<ZoneRegion> loadedRegions;
                if (formatVersion == LEGACY_FORMAT_VERSION) {
                    if (min == null || !min.isComplete() || max == null || !max.isComplete()) {
                        throw new IOException("Incomplete legacy zone entry in " + source);
                    }
                    loadedRegions = List.of(new ZoneRegion(min.toPosition(), max.toPosition()));
                } else {
                    if (regions == null || regions.isEmpty()) {
                        throw new IOException("Zone '" + name + "' has no regions in " + source);
                    }
                    loadedRegions = new ArrayList<>(regions.size());
                    for (RegionData region : regions) {
                        if (region == null || !region.isComplete()) {
                            throw new IOException("Incomplete region in zone '" + name + "' in " + source);
                        }
                        loadedRegions.add(region.toRegion());
                    }
                }

                return new Zone(
                        UUID.fromString(id),
                        name,
                        world,
                        loadedRegions,
                        protection.toProtection()
                );
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new IOException("Invalid zone entry '" + name + "' in " + source, exception);
            }
        }

        private static final class RegionData {
            private PositionData min;
            private PositionData max;

            private RegionData() {
            }

            private RegionData(PositionData min, PositionData max) {
                this.min = min;
                this.max = max;
            }

            private static RegionData from(ZoneRegion region) {
                return new RegionData(
                        PositionData.from(region.min()),
                        PositionData.from(region.max())
                );
            }

            private boolean isComplete() {
                return min != null && min.isComplete() && max != null && max.isComplete();
            }

            private ZoneRegion toRegion() {
                return new ZoneRegion(min.toPosition(), max.toPosition());
            }
        }
    }

    private static final class PositionData {
        private Integer x;
        private Integer y;
        private Integer z;

        private PositionData() {
        }

        private PositionData(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private static PositionData from(BlockPosition position) {
            return new PositionData(position.x(), position.y(), position.z());
        }

        private boolean isComplete() {
            return x != null && y != null && z != null;
        }

        private BlockPosition toPosition() {
            return new BlockPosition(x, y, z);
        }
    }

    private static final class ProtectionData {
        private Boolean breakBlocks;
        private Boolean placeBlocks;
        private Boolean damageBlocks;

        private ProtectionData() {
        }

        private ProtectionData(boolean breakBlocks, boolean placeBlocks, boolean damageBlocks) {
            this.breakBlocks = breakBlocks;
            this.placeBlocks = placeBlocks;
            this.damageBlocks = damageBlocks;
        }

        private static ProtectionData from(ZoneProtection protection) {
            return new ProtectionData(
                    protection.breakBlocks(),
                    protection.placeBlocks(),
                    protection.damageBlocks()
            );
        }

        private boolean isComplete() {
            return breakBlocks != null && placeBlocks != null && damageBlocks != null;
        }

        private ZoneProtection toProtection() {
            return new ZoneProtection(breakBlocks, placeBlocks, damageBlocks);
        }
    }
}
