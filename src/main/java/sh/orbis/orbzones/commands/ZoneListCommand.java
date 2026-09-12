package sh.orbis.orbzones.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.api.Zone;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

final class ZoneListCommand extends CommandBase {
    private final ZoneManager zoneManager;

    ZoneListCommand(@Nonnull ZoneManager zoneManager) {
        super("list", "List all zones");
        this.zoneManager = zoneManager;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (zoneManager.getZones().isEmpty()) {
            CommandSupport.send(context, "No zones are configured.");
            return;
        }

        String names = zoneManager.getZones().stream()
                .map(Zone::name)
                .collect(Collectors.joining(", "));
        CommandSupport.send(context, "Zones: " + names);
    }
}
