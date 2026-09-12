package sh.orbis.orbzones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sh.orbis.orbzones.ZoneSelectionService;
import sh.orbis.orbzones.ZoneVisualization;
import sh.orbis.orbzones.api.BlockPosition;

import javax.annotation.Nonnull;

final class ZonePositionCommand extends AbstractPlayerCommand {
    private final ZoneSelectionService selections;
    private final Corner corner;

    ZonePositionCommand(@Nonnull ZoneSelectionService selections, @Nonnull Corner corner) {
        super(corner.commandName, corner.description);
        this.selections = selections;
        this.corner = corner;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntity,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        TransformComponent transform = store.getComponent(
                playerEntity,
                TransformComponent.getComponentType()
        );
        if (transform == null) {
            CommandSupport.send(context, "Your current position is unavailable.");
            return;
        }

        BlockPosition position = BlockPosition.from(transform.getPosition());
        if (corner == Corner.FIRST) {
            selections.setFirst(playerRef.getUuid(), world.getName(), position);
        } else {
            selections.setSecond(playerRef.getUuid(), world.getName(), position);
        }
        selections.getPreview(playerRef.getUuid(), world.getName())
                .ifPresent(region -> ZoneVisualization.showSelection(playerRef, region));
        CommandSupport.send(
                context,
                corner.displayName + " zone corner set to " + CommandSupport.format(position)
        );
    }

    enum Corner {
        FIRST("pos1", "Set the first zone corner", "First"),
        SECOND("pos2", "Set the second zone corner", "Second");

        private final String commandName;
        private final String description;
        private final String displayName;

        Corner(String commandName, String description, String displayName) {
            this.commandName = commandName;
            this.description = description;
            this.displayName = displayName;
        }
    }
}
