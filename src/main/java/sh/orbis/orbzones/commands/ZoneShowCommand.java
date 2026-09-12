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
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.ZoneVisualization;
import sh.orbis.orbzones.api.Zone;

import javax.annotation.Nonnull;

final class ZoneShowCommand extends AbstractPlayerCommand {
    private final ZoneManager zoneManager;
    private final RequiredArg<String> nameArgument;

    ZoneShowCommand(@Nonnull ZoneManager zoneManager) {
        super("show", "Show a zone boundary");
        this.zoneManager = zoneManager;
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
        String name = context.get(nameArgument);
        Zone zone = zoneManager.getByName(name).orElse(null);
        if (zone == null) {
            CommandSupport.send(context, "Unknown zone '" + name + "'.");
            return;
        }
        if (!zone.worldName().equals(world.getName())) {
            CommandSupport.send(
                    context,
                    "Zone '" + zone.name() + "' is in world '" + zone.worldName() + "'."
            );
            return;
        }

        for (var packet : ZoneVisualization.createPackets(zone)) {
            playerRef.getPacketHandler().write(packet);
        }
        CommandSupport.send(
                context,
                "Showing " + zone.regions().size() + " region(s) in zone '" + zone.name() + "' for "
                        + (int) ZoneVisualization.DISPLAY_SECONDS + " seconds."
        );
    }
}
