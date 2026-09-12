package sh.orbis.orbzones;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import sh.orbis.orbzones.commands.ZoneCommand;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneTransitionEvent;
import sh.orbis.orbzones.storage.ZoneStorage;
import sh.orbis.orbzones.systems.ZoneProtectionSystems;
import sh.orbis.orbzones.systems.ZoneTransitionSystem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

public final class OrbZonesPlugin extends JavaPlugin {
    private static volatile OrbZonesPlugin instance;

    private ZoneManager zoneManager;
    private ZoneSelectionService selections;
    private ZoneMembershipTracker memberships;

    public OrbZonesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Nonnull
    public static OrbZonesPlugin get() {
        OrbZonesPlugin current = instance;
        if (current == null) {
            throw new IllegalStateException("OrbZones is not running");
        }
        return current;
    }

    @Nonnull
    public ZoneManager getZoneManager() {
        if (zoneManager == null) {
            throw new IllegalStateException("OrbZones has not finished setup");
        }
        return zoneManager;
    }

    @Override
    protected void setup() {
        try {
            zoneManager = new ZoneManager(
                    new ZoneStorage(getDataDirectory().resolve("zones.json")),
                    (transition, event, exception) -> getLogger().at(Level.SEVERE)
                            .withCause(exception)
                            .log(
                                    "Zone listener failed during %s for player %s in zone %s",
                                    transition,
                                    event.player().getUsername(),
                                    event.zone().name()
                            )
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load OrbZones data", exception);
        }

        selections = new ZoneSelectionService();
        memberships = new ZoneMembershipTracker();
        instance = this;

        getCommandRegistry().registerCommand(new ZoneCommand(zoneManager, selections, getLogger()));
        getEntityStoreRegistry().registerSystem(new ZoneProtectionSystems.Break(zoneManager));
        getEntityStoreRegistry().registerSystem(new ZoneProtectionSystems.Place(zoneManager));
        getEntityStoreRegistry().registerSystem(new ZoneProtectionSystems.Damage(zoneManager));
        getEntityStoreRegistry().registerSystem(new ZoneTransitionSystem(zoneManager, memberships));
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            var playerRef = event.getPlayerRef();
            selections.clear(playerRef.getUuid());
            BlockPosition position = BlockPosition.from(playerRef.getTransform().getPosition());
            for (Zone zone : memberships.remove(playerRef.getUuid())) {
                zoneManager.dispatchLeave(new ZoneTransitionEvent(playerRef, zone, position));
            }
        });

        getLogger().at(Level.INFO).log("Loaded %d zones", zoneManager.getZones().size());
    }

    @Override
    protected void shutdown() {
        instance = null;
    }
}
