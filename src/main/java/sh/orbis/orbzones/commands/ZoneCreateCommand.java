package sh.orbis.orbzones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.ZoneSelectionService;
import sh.orbis.orbzones.ZoneVisualization;
import sh.orbis.orbzones.api.Zone;

import javax.annotation.Nonnull;
import java.io.IOException;

final class ZoneCreateCommand extends AbstractPlayerCommand {
    private final ZoneManager zoneManager;
    private final ZoneSelectionService selections;
    private final HytaleLogger logger;
    private final RequiredArg<String> nameArgument;

    ZoneCreateCommand(
            @Nonnull ZoneManager zoneManager,
            @Nonnull ZoneSelectionService selections,
            @Nonnull HytaleLogger logger
    ) {
        super("create", "Create a zone from the current selection");
        this.zoneManager = zoneManager;
        this.selections = selections;
        this.logger = logger;
        this.nameArgument = withRequiredArg("name", "Zone name", ArgTypes.STRING);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntity,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        ZoneSelectionService.Selection selection;
        try {
            selection = selections.getComplete(playerRef.getUuid()).orElse(null);
        } catch (IllegalStateException exception) {
            CommandSupport.send(context, exception.getMessage());
            return;
        }
        if (selection == null) {
            CommandSupport.send(context, "Set both corners with /zone pos1 and /zone pos2 first.");
            return;
        }
        if (!selection.worldName().equals(world.getName())) {
            CommandSupport.send(context, "Return to the selected world before creating this zone.");
            return;
        }

        try {
            String name = context.get(nameArgument);
            Zone zone = zoneManager.create(
                    name,
                    selection.worldName(),
                    selection.first(),
                    selection.second()
            );
            selections.clear(playerRef.getUuid());
            ZoneVisualization.clear(playerRef);
            if (zone.regions().size() == 1) {
                CommandSupport.send(
                        context,
                        "Created zone '" + zone.name() + "' with 1 region and "
                                + zone.volume() + " blocks."
                );
            } else {
                CommandSupport.send(
                        context,
                        "Added region " + zone.regions().size() + " to zone '" + zone.name() + "'."
                );
            }
        } catch (IllegalArgumentException exception) {
            CommandSupport.send(context, exception.getMessage());
        } catch (IOException exception) {
            CommandSupport.reportStorageFailure(context, logger, "create", exception);
        }
    }
}
