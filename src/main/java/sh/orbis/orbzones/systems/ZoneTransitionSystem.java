package sh.orbis.orbzones.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.ZoneMembershipTracker;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneTransitionEvent;

import javax.annotation.Nonnull;
import java.util.List;

public final class ZoneTransitionSystem extends EntityTickingSystem<EntityStore> {
    private final ZoneManager zoneManager;
    private final ZoneMembershipTracker memberships;

    public ZoneTransitionSystem(
            @Nonnull ZoneManager zoneManager,
            @Nonnull ZoneMembershipTracker memberships
    ) {
        this.zoneManager = zoneManager;
        this.memberships = memberships;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public boolean isParallel(int entityCount, int taskCount) {
        return false;
    }

    @Override
    public void tick(
            float deltaTime,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (playerRef == null || transform == null) {
            return;
        }

        World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
        BlockPosition position = BlockPosition.from(transform.getPosition());
        List<Zone> currentZones = zoneManager.findContaining(world.getName(), position);
        ZoneMembershipTracker.MembershipChange change = memberships.update(
                playerRef.getUuid(),
                currentZones
        );

        for (Zone zone : change.left()) {
            zoneManager.dispatchLeave(new ZoneTransitionEvent(playerRef, zone, position));
        }
        for (Zone zone : change.entered()) {
            zoneManager.dispatchEnter(new ZoneTransitionEvent(playerRef, zone, position));
        }
    }
}
