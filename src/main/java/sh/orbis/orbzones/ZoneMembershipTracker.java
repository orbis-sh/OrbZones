package sh.orbis.orbzones;

import sh.orbis.orbzones.api.Zone;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ZoneMembershipTracker {
    private final ConcurrentHashMap<UUID, Map<UUID, Zone>> memberships = new ConcurrentHashMap<>();

    @Nonnull
    public MembershipChange update(@Nonnull UUID playerId, @Nonnull Collection<Zone> currentMemberships) {
        Map<UUID, Zone> current = index(currentMemberships);
        Map<UUID, Zone> previous;
        if (current.isEmpty()) {
            previous = memberships.remove(playerId);
        } else {
            previous = memberships.put(playerId, Map.copyOf(current));
        }
        if (previous == null) {
            previous = Map.of();
        }

        Set<Zone> entered = new LinkedHashSet<>();
        for (Map.Entry<UUID, Zone> entry : current.entrySet()) {
            if (!previous.containsKey(entry.getKey())) {
                entered.add(entry.getValue());
            }
        }

        Set<Zone> left = new LinkedHashSet<>();
        for (Map.Entry<UUID, Zone> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                left.add(entry.getValue());
            }
        }
        return new MembershipChange(Set.copyOf(entered), Set.copyOf(left));
    }

    @Nonnull
    public Set<Zone> remove(@Nonnull UUID playerId) {
        Map<UUID, Zone> removed = memberships.remove(playerId);
        return removed == null ? Set.of() : Set.copyOf(removed.values());
    }

    private static Map<UUID, Zone> index(Collection<Zone> zones) {
        Map<UUID, Zone> indexed = new LinkedHashMap<>();
        for (Zone zone : zones) {
            indexed.put(zone.id(), zone);
        }
        return indexed;
    }

    public record MembershipChange(@Nonnull Set<Zone> entered, @Nonnull Set<Zone> left) {
        public boolean isEmpty() {
            return entered.isEmpty() && left.isEmpty();
        }
    }
}
