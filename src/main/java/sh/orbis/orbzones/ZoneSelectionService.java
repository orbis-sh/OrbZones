package sh.orbis.orbzones;

import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.ZoneRegion;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ZoneSelectionService {
    private final ConcurrentHashMap<UUID, SelectionState> selections = new ConcurrentHashMap<>();

    public void setFirst(@Nonnull UUID playerId, @Nonnull String worldName, @Nonnull BlockPosition position) {
        selections.compute(playerId, (ignored, existing) -> new SelectionState(
                worldName,
                position,
                existing == null ? null : existing.secondWorld(),
                existing == null ? null : existing.second()
        ));
    }

    public void setSecond(@Nonnull UUID playerId, @Nonnull String worldName, @Nonnull BlockPosition position) {
        selections.compute(playerId, (ignored, existing) -> new SelectionState(
                existing == null ? null : existing.firstWorld(),
                existing == null ? null : existing.first(),
                worldName,
                position
        ));
    }

    @Nonnull
    public Selection setHeight(@Nonnull UUID playerId, int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Selection height must be greater than zero.");
        }

        SelectionState updated = selections.compute(playerId, (ignored, existing) -> {
            Selection selection = requireComplete(existing);
            int minY = Math.min(selection.first().y(), selection.second().y());
            int maxY;
            try {
                maxY = Math.addExact(minY, height - 1);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Selection height is too large.", exception);
            }

            return new SelectionState(
                    selection.worldName(),
                    new BlockPosition(selection.first().x(), minY, selection.first().z()),
                    selection.worldName(),
                    new BlockPosition(selection.second().x(), maxY, selection.second().z())
            );
        });
        return requireComplete(updated);
    }

    @Nonnull
    public Optional<Selection> getComplete(@Nonnull UUID playerId) {
        SelectionState selection = selections.get(playerId);
        if (selection == null || selection.first() == null || selection.second() == null) {
            return Optional.empty();
        }
        return Optional.of(requireComplete(selection));
    }

    @Nonnull
    public Optional<ZoneRegion> getPreview(@Nonnull UUID playerId, @Nonnull String worldName) {
        SelectionState selection = selections.get(playerId);
        if (selection == null) {
            return Optional.empty();
        }

        BlockPosition first = worldName.equals(selection.firstWorld()) ? selection.first() : null;
        BlockPosition second = worldName.equals(selection.secondWorld()) ? selection.second() : null;
        if (first == null && second == null) {
            return Optional.empty();
        }
        if (first == null) {
            first = second;
        } else if (second == null) {
            second = first;
        }
        return Optional.of(new ZoneRegion(first, second));
    }

    public void clear(@Nonnull UUID playerId) {
        selections.remove(playerId);
    }

    public record Selection(
            @Nonnull String worldName,
            @Nonnull BlockPosition first,
            @Nonnull BlockPosition second
    ) {
    }

    @Nonnull
    private static Selection requireComplete(SelectionState selection) {
        if (selection == null || selection.first() == null || selection.second() == null) {
            throw new IllegalStateException("Set both corners with /zone pos1 and /zone pos2 first.");
        }
        if (!selection.firstWorld().equals(selection.secondWorld())) {
            throw new IllegalStateException("Both selection points must be in the same world");
        }
        return new Selection(selection.firstWorld(), selection.first(), selection.second());
    }

    private record SelectionState(
            String firstWorld,
            BlockPosition first,
            String secondWorld,
            BlockPosition second
    ) {
    }
}
