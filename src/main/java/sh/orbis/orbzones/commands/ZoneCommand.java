package sh.orbis.orbzones.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.ZoneSelectionService;

import javax.annotation.Nonnull;

public final class ZoneCommand extends AbstractCommandCollection {
    public static final String ADMIN_PERMISSION = "orbzones.admin";

    public ZoneCommand(
            @Nonnull ZoneManager zoneManager,
            @Nonnull ZoneSelectionService selections,
            @Nonnull HytaleLogger logger
    ) {
        super("zone", "Manage protected cuboid zones");
        requirePermission(ADMIN_PERMISSION);
        addAdminSubCommand(new ZonePositionCommand(selections, ZonePositionCommand.Corner.FIRST));
        addAdminSubCommand(new ZonePositionCommand(selections, ZonePositionCommand.Corner.SECOND));
        addAdminSubCommand(new ZoneHeightCommand(selections));
        addAdminSubCommand(new ZoneCreateCommand(zoneManager, selections, logger));
        addAdminSubCommand(new ZoneDeleteCommand(zoneManager, logger));
        addAdminSubCommand(new ZoneListCommand(zoneManager));
        addAdminSubCommand(new ZoneInfoCommand(zoneManager));
        addAdminSubCommand(new ZoneShowCommand(zoneManager));
        addAdminSubCommand(new ZoneProtectCommand(zoneManager, logger));
    }

    private void addAdminSubCommand(AbstractCommand command) {
        command.requirePermission(ADMIN_PERMISSION);
        addSubCommand(command);
    }
}
