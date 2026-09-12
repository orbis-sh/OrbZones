package sh.orbis.orbzones.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import sh.orbis.orbzones.ZoneManager;
import sh.orbis.orbzones.api.Zone;
import sh.orbis.orbzones.api.ZoneAction;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Locale;

final class ZoneProtectCommand extends CommandBase {
    private final ZoneManager zoneManager;
    private final HytaleLogger logger;
    private final RequiredArg<String> nameArgument;
    private final RequiredArg<String> actionArgument;
    private final RequiredArg<Boolean> valueArgument;

    ZoneProtectCommand(@Nonnull ZoneManager zoneManager, @Nonnull HytaleLogger logger) {
        super("protect", "Enable or disable a zone protection action");
        this.zoneManager = zoneManager;
        this.logger = logger;
        this.nameArgument = withRequiredArg("name", "Zone name", ArgTypes.STRING);
        this.actionArgument = withRequiredArg("action", "break, place, or damage", ArgTypes.STRING);
        this.valueArgument = withRequiredArg("enabled", "Protection state", ArgTypes.BOOLEAN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String actionInput = context.get(actionArgument);
        ZoneAction action;
        try {
            action = ZoneAction.valueOf(actionInput.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            CommandSupport.send(context, "Action must be break, place, or damage.");
            return;
        }

        try {
            Zone zone = zoneManager.setProtection(
                    context.get(nameArgument),
                    action,
                    context.get(valueArgument)
            );
            CommandSupport.send(
                    context,
                    actionInput.toLowerCase(Locale.ROOT) + " protection for '" + zone.name() + "' is now "
                            + zone.protection().protects(action) + "."
            );
        } catch (IllegalArgumentException exception) {
            CommandSupport.send(context, exception.getMessage());
        } catch (IOException exception) {
            CommandSupport.reportStorageFailure(context, logger, "update", exception);
        }
    }
}
