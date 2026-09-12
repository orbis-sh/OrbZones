package sh.orbis.orbzones.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public record ZoneTransitionEvent(
        @Nonnull PlayerRef player,
        @Nonnull Zone zone,
        @Nonnull BlockPosition position
) {
}
