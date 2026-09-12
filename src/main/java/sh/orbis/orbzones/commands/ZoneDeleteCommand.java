package sh.orbis.orbzones.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import sh.orbis.orbzones.ZoneManager;

import javax.annotation.Nonnull;
import java.io.IOException;

final class ZoneDeleteCommand extends CommandBase {
    private final ZoneManager zoneManager;
    private final HytaleLogger logger;
    private final RequiredArg<String> nameArgument;

    ZoneDeleteCommand(@Nonnull ZoneManager zoneManager, @Nonnull HytaleLogger logger) {
        super("delete", "Delete a zone");
        this.zoneManager = zoneManager;
        this.logger = logger;
        this.nameArgument = withRequiredArg("name", "Zone name", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        String name = context.get(nameArgument);
        try {
            if (zoneManager.delete(name)) {
                CommandSupport.send(context, "Deleted zone '" + name + "'.");
            } else {
                CommandSupport.send(context, "Unknown zone '" + name + "'.");
            }
        } catch (IOException exception) {
            CommandSupport.reportStorageFailure(context, logger, "delete", exception);
        }
    }
}
