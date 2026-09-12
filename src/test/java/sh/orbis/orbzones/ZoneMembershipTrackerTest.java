package sh.orbis.orbzones;

import org.junit.jupiter.api.Test;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneAction;
import sh.orbis.orbzones.api.ZoneProtection;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneMembershipTrackerTest {
    @Test
    void reportsOnlyMembershipTransitions() {
        ZoneMembershipTracker tracker = new ZoneMembershipTracker();
        UUID playerId = UUID.randomUUID();
        Zone first = zone("first");
        Zone second = zone("second");

        ZoneMembershipTracker.MembershipChange entered = tracker.update(playerId, List.of(first, second));
        assertEquals(2, entered.entered().size());
        assertTrue(entered.left().isEmpty());

        Zone updatedFirst = first.withProtection(first.protection().with(ZoneAction.BREAK, false));
        ZoneMembershipTracker.MembershipChange changedFlags = tracker.update(
                playerId,
                List.of(updatedFirst, second)
        );
        assertTrue(changedFlags.isEmpty());

        ZoneMembershipTracker.MembershipChange left = tracker.update(playerId, List.of(second));
        assertEquals(first.id(), left.left().iterator().next().id());
        assertTrue(left.entered().isEmpty());
        assertEquals(Set.of(second), tracker.remove(playerId));
        assertTrue(tracker.remove(playerId).isEmpty());
    }

    private static Zone zone(String name) {
        return new Zone(
                UUID.randomUUID(),
                name,
                "default",
                new BlockPosition(0, 0, 0),
                new BlockPosition(10, 10, 10),
                ZoneProtection.ALL
        );
    }
}
