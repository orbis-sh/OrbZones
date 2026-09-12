package sh.orbis.orbzones.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3ic;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.ZoneAction;

import javax.annotation.Nonnull;
import java.util.function.Function;

public final class ZoneProtectionSystems {
    public static final String BYPASS_PERMISSION = "orbzones.bypass";

    private ZoneProtectionSystems() {
    }

    public static final class Break extends GuardSystem<BreakBlockEvent> {
        public Break(@Nonnull ZoneManager zoneManager) {
            super(BreakBlockEvent.class, zoneManager, ZoneAction.BREAK, BreakBlockEvent::getTargetBlock);
        }
    }

    public static final class Place extends GuardSystem<PlaceBlockEvent> {
        public Place(@Nonnull ZoneManager zoneManager) {
            super(PlaceBlockEvent.class, zoneManager, ZoneAction.PLACE, PlaceBlockEvent::getTargetBlock);
        }
    }

    public static final class Damage extends GuardSystem<DamageBlockEvent> {
        public Damage(@Nonnull ZoneManager zoneManager) {
            super(DamageBlockEvent.class, zoneManager, ZoneAction.DAMAGE, DamageBlockEvent::getTargetBlock);
        }
    }

    private abstract static class GuardSystem<E extends CancellableEcsEvent>
            extends EntityEventSystem<EntityStore, E> {
        private final ZoneManager zoneManager;
        private final ZoneAction action;
        private final Function<E, Vector3ic> targetPosition;

        protected GuardSystem(
                Class<E> eventType,
                ZoneManager zoneManager,
                ZoneAction action,
                Function<E, Vector3ic> targetPosition
        ) {
            super(eventType);
            this.zoneManager = zoneManager;
            this.action = action;
            this.targetPosition = targetPosition;
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }

        @Override
        public void handle(
                int index,
                @Nonnull ArchetypeChunk<EntityStore> chunk,
                @Nonnull Store<EntityStore> store,
                @Nonnull CommandBuffer<EntityStore> commandBuffer,
                @Nonnull E event
        ) {
            PlayerRef playerRef = store.getComponent(
                    chunk.getReferenceTo(index),
                    PlayerRef.getComponentType()
            );
            if (playerRef != null && playerRef.hasPermission(BYPASS_PERMISSION)) {
                return;
            }

            World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
            BlockPosition position = BlockPosition.from(targetPosition.apply(event));
            if (zoneManager.isProtected(world.getName(), position, action)) {
                event.setCancelled(true);
            }
        }
    }
}
