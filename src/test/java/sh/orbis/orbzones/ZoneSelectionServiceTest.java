package sh.orbis.orbzones;

import org.junit.jupiter.api.Test;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.ZoneRegion;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneSelectionServiceTest {
    @Test
    void returnsACompleteSelectionInOneWorld() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        selections.setFirst(playerId, "default", new BlockPosition(1, 2, 3));
        selections.setSecond(playerId, "default", new BlockPosition(4, 5, 6));

        ZoneSelectionService.Selection selection = selections.getComplete(playerId).orElseThrow();

        assertEquals("default", selection.worldName());
        assertEquals(new BlockPosition(1, 2, 3), selection.first());
        assertEquals(new BlockPosition(4, 5, 6), selection.second());
    }

    @Test
    void rejectsCornersFromDifferentWorlds() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        selections.setFirst(playerId, "default", new BlockPosition(1, 2, 3));
        selections.setSecond(playerId, "dungeon", new BlockPosition(4, 5, 6));

        assertThrows(IllegalStateException.class, () -> selections.getComplete(playerId));
        selections.clear(playerId);
        assertTrue(selections.getComplete(playerId).isEmpty());
    }

    @Test
    void setsAnExactHeightFromTheLowerSelectedY() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        selections.setFirst(playerId, "default", new BlockPosition(1, 12, 3));
        selections.setSecond(playerId, "default", new BlockPosition(4, 10, 6));

        ZoneSelectionService.Selection selection = selections.setHeight(playerId, 5);

        assertEquals(new BlockPosition(1, 10, 3), selection.first());
        assertEquals(new BlockPosition(4, 14, 6), selection.second());
    }

    @Test
    void rejectsInvalidHeight() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        selections.setFirst(playerId, "default", new BlockPosition(1, 2, 3));
        selections.setSecond(playerId, "default", new BlockPosition(4, 5, 6));

        assertThrows(IllegalArgumentException.class, () -> selections.setHeight(playerId, 0));
    }

    @Test
    void requiresBothCornersBeforeSettingHeight() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        selections.setFirst(playerId, "default", new BlockPosition(1, 2, 3));

        assertThrows(IllegalStateException.class, () -> selections.setHeight(playerId, 5));
    }

    @Test
    void previewsOneCornerAsOneBlock() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        BlockPosition position = new BlockPosition(1, 2, 3);
        selections.setFirst(playerId, "default", position);

        assertEquals(
                new ZoneRegion(position, position),
                selections.getPreview(playerId, "default").orElseThrow()
        );
    }

    @Test
    void previewsBothCornersInTheCurrentWorld() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        selections.setFirst(playerId, "default", new BlockPosition(4, 6, 8));
        selections.setSecond(playerId, "default", new BlockPosition(1, 2, 3));

        assertEquals(
                new ZoneRegion(new BlockPosition(1, 2, 3), new BlockPosition(4, 6, 8)),
                selections.getPreview(playerId, "default").orElseThrow()
        );
    }

    @Test
    void previewsOnlyTheCornerInTheCurrentWorld() {
        ZoneSelectionService selections = new ZoneSelectionService();
        UUID playerId = UUID.randomUUID();
        BlockPosition currentWorldPosition = new BlockPosition(4, 5, 6);
        selections.setFirst(playerId, "default", new BlockPosition(1, 2, 3));
        selections.setSecond(playerId, "dungeon", currentWorldPosition);

        assertEquals(
                new ZoneRegion(currentWorldPosition, currentWorldPosition),
                selections.getPreview(playerId, "dungeon").orElseThrow()
        );
    }
}
