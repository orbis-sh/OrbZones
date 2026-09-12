package sh.orbis.orbzones.api;

import javax.annotation.Nonnull;

public record ZoneProtection(boolean breakBlocks, boolean placeBlocks, boolean damageBlocks) {
    public static final ZoneProtection ALL = new ZoneProtection(true, true, true);
    public static final ZoneProtection NONE = new ZoneProtection(false, false, false);

    public boolean protects(@Nonnull ZoneAction action) {
        return switch (action) {
            case BREAK -> breakBlocks;
            case PLACE -> placeBlocks;
            case DAMAGE -> damageBlocks;
        };
    }

    @Nonnull
    public ZoneProtection with(@Nonnull ZoneAction action, boolean protectedValue) {
        return switch (action) {
            case BREAK -> new ZoneProtection(protectedValue, placeBlocks, damageBlocks);
            case PLACE -> new ZoneProtection(breakBlocks, protectedValue, damageBlocks);
            case DAMAGE -> new ZoneProtection(breakBlocks, placeBlocks, protectedValue);
        };
    }
}
