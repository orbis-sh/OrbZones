package sh.orbis.orbzones.api;

import javax.annotation.Nonnull;

public interface ZoneListener {
    default void onEnter(@Nonnull ZoneTransitionEvent event) {
    }

    default void onLeave(@Nonnull ZoneTransitionEvent event) {
    }
}
