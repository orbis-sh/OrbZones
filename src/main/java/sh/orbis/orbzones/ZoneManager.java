package sh.orbis.orbzones;

import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneAction;
import sh.orbis.orbzones.api.ZoneListener;
import sh.orbis.orbzones.api.ZoneListenerRegistration;
import sh.orbis.orbzones.api.ZoneProtection;
import sh.orbis.orbzones.api.ZoneRegion;
import sh.orbis.orbzones.api.ZoneTransitionEvent;
import sh.orbis.orbzones.storage.ZoneStorage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class ZoneManager {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final ZoneStorage storage;
    private final ListenerFailureHandler listenerFailureHandler;
    private final CopyOnWriteArrayList<ZoneListener> listeners = new CopyOnWriteArrayList<>();
    private volatile List<Zone> zones;

    public ZoneManager(
            @Nonnull ZoneStorage storage,
            @Nonnull ListenerFailureHandler listenerFailureHandler
    ) throws IOException {
        this.storage = storage;
        this.listenerFailureHandler = listenerFailureHandler;
        this.zones = validate(storage.load());
    }

    @Nonnull
    public List<Zone> getZones() {
        return zones;
    }

    @Nonnull
    public Optional<Zone> getById(@Nonnull UUID id) {
        return zones.stream().filter(zone -> zone.id().equals(id)).findFirst();
    }

    @Nonnull
    public Optional<Zone> getByName(@Nonnull String name) {
        return zones.stream().filter(zone -> zone.name().equalsIgnoreCase(name)).findFirst();
    }

    @Nonnull
    public List<Zone> findContaining(@Nonnull String worldName, @Nonnull BlockPosition position) {
        return zones.stream().filter(zone -> zone.contains(worldName, position)).toList();
    }

    public boolean isProtected(
            @Nonnull String worldName,
            @Nonnull BlockPosition position,
            @Nonnull ZoneAction action
    ) {
        return zones.stream()
                .anyMatch(zone -> zone.contains(worldName, position) && zone.protection().protects(action));
    }

    @Nonnull
    public synchronized Zone create(
            @Nonnull String name,
            @Nonnull String worldName,
            @Nonnull BlockPosition firstCorner,
            @Nonnull BlockPosition secondCorner
    ) throws IOException {
        validateName(name);
        Zone existing = getByName(name).orElse(null);
        if (existing != null) {
            if (!existing.worldName().equals(worldName)) {
                throw new IllegalArgumentException(
                        "Zone '" + existing.name() + "' belongs to world '" + existing.worldName() + "'"
                );
            }

            Zone replacement = existing.withRegion(new ZoneRegion(firstCorner, secondCorner));
            List<Zone> updated = zones.stream()
                    .map(zone -> zone.id().equals(existing.id()) ? replacement : zone)
                    .toList();
            publish(updated);
            return replacement;
        }

        Zone zone = new Zone(
                UUID.randomUUID(),
                name,
                worldName,
                firstCorner,
                secondCorner,
                ZoneProtection.ALL
        );
        List<Zone> updated = new ArrayList<>(zones);
        updated.add(zone);
        publish(updated);
        return zone;
    }

    public synchronized boolean delete(@Nonnull String name) throws IOException {
        List<Zone> updated = zones.stream()
                .filter(zone -> !zone.name().equalsIgnoreCase(name))
                .toList();
        if (updated.size() == zones.size()) {
            return false;
        }
        publish(updated);
        return true;
    }

    @Nonnull
    public synchronized Zone setProtection(
            @Nonnull String name,
            @Nonnull ZoneAction action,
            boolean protectedValue
    ) throws IOException {
        Zone existing = getByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown zone '" + name + "'"));
        Zone replacement = existing.withProtection(existing.protection().with(action, protectedValue));
        List<Zone> updated = zones.stream()
                .map(zone -> zone.id().equals(existing.id()) ? replacement : zone)
                .toList();
        publish(updated);
        return replacement;
    }

    @Nonnull
    public ZoneListenerRegistration registerListener(@Nonnull ZoneListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void dispatchEnter(@Nonnull ZoneTransitionEvent event) {
        dispatch(listener -> listener.onEnter(event), "enter", event);
    }

    public void dispatchLeave(@Nonnull ZoneTransitionEvent event) {
        dispatch(listener -> listener.onLeave(event), "leave", event);
    }

    private void dispatch(
            Consumer<ZoneListener> callback,
            String transition,
            ZoneTransitionEvent event
    ) {
        for (ZoneListener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (RuntimeException exception) {
                listenerFailureHandler.report(transition, event, exception);
            }
        }
    }

    private void publish(List<Zone> updated) throws IOException {
        List<Zone> validated = validate(updated);
        storage.save(validated);
        zones = validated;
    }

    private static List<Zone> validate(List<Zone> candidateZones) throws IOException {
        Set<UUID> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Zone zone : candidateZones) {
            try {
                validateName(zone.name());
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid stored zone name: " + zone.name(), exception);
            }
            if (!ids.add(zone.id())) {
                throw new IOException("Duplicate stored zone id: " + zone.id());
            }
            if (!names.add(zone.name().toLowerCase(Locale.ROOT))) {
                throw new IOException("Duplicate stored zone name: " + zone.name());
            }
        }

        return candidateZones.stream()
                .sorted(Comparator.comparing(Zone::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static void validateName(String name) {
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Zone names must contain 1-32 letters, numbers, underscores, or hyphens"
            );
        }
    }

    @FunctionalInterface
    public interface ListenerFailureHandler {
        void report(
                @Nonnull String transition,
                @Nonnull ZoneTransitionEvent event,
                @Nonnull RuntimeException exception
        );
    }
}
