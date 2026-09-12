package sh.orbis.orbzones.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneRegion;

import javax.annotation.Nonnull;

final class ZoneInfoCommand extends CommandBase {
    private final ZoneManager zoneManager;
    private final RequiredArg<String> nameArgument;

    ZoneInfoCommand(@Nonnull ZoneManager zoneManager) {
        super("info", "Show zone details");
        this.zoneManager = zoneManager;
        this.nameArgument = withRequiredArg("name", "Zone name", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String name = context.get(nameArgument);
        Zone zone = zoneManager.getByName(name).orElse(null);
        if (zone == null) {
            CommandSupport.send(context, "Unknown zone '" + name + "'.");
            return;
        }

        CommandSupport.send(
                context,
                "Zone '" + zone.name() + "' | world=" + zone.worldName()
                        + " | regions=" + zone.regions().size()
                        + " | break=" + zone.protection().breakBlocks()
                        + " | place=" + zone.protection().placeBlocks()
                        + " | damage=" + zone.protection().damageBlocks()
        );
        for (int index = 0; index < zone.regions().size(); index++) {
            ZoneRegion region = zone.regions().get(index);
            CommandSupport.send(
                    context,
                    "Region " + (index + 1)
                            + " | min=" + CommandSupport.format(region.min())
                            + " | max=" + CommandSupport.format(region.max())
                            + " | blocks=" + region.volume()
            );
        }
    }
}
