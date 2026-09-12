package sh.orbis.orbzones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sh.orbis.orbzones.ZoneSelectionService;
import sh.orbis.orbzones.ZoneVisualization;
import sh.orbis.orbzones.api.ZoneRegion;

import javax.annotation.Nonnull;

final class ZoneHeightCommand extends AbstractPlayerCommand {
    private final ZoneSelectionService selections;
    private final RequiredArg<Integer> heightArgument;

    ZoneHeightCommand(@Nonnull ZoneSelectionService selections) {
        super("height", "Set the current selection height");
        this.selections = selections;
        this.heightArgument = withRequiredArg("blocks", "Selection height in blocks", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntity,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        try {
            ZoneSelectionService.Selection selection = selections.setHeight(
                    playerRef.getUuid(),
                    context.get(heightArgument)
            );
            int minY = Math.min(selection.first().y(), selection.second().y());
            int maxY = Math.max(selection.first().y(), selection.second().y());
            ZoneVisualization.showSelection(
                    playerRef,
                    new ZoneRegion(selection.first(), selection.second())
            );
            CommandSupport.send(
                    context,
                    "Selection height set to " + (maxY - minY + 1)
                            + " blocks (Y " + minY + " through " + maxY + ")."
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            CommandSupport.send(context, exception.getMessage());
        }
    }
}
