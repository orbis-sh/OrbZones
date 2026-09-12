package sh.orbis.orbzones.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import sh.orbis.orbzones.api.BlockPosition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

final class CommandSupport {
    private CommandSupport() {
    }

    static void send(@Nonnull CommandContext context, @Nonnull String message) {
        context.sendMessage(Message.raw(message));
    }

    static String format(@Nonnull BlockPosition position) {
        return position.x() + ", " + position.y() + ", " + position.z();
    }

    static void reportStorageFailure(
            @Nonnull CommandContext context,
            @Nonnull HytaleLogger logger,
            @Nonnull String operation,
            @Nonnull IOException exception
    ) {
        logger.at(Level.SEVERE)
                .withCause(exception)
                .log("Failed to %s zone data", operation);
        send(context, "Zone data could not be saved. Check the server log.");
    }
}
