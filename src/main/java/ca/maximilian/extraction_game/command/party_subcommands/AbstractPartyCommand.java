package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.PartyException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

/**
 * Common groundwork for every "/party ..." subcommand: the player check, the
 * error/success message plus sound combo, the "find one player from an
 * ArgumentEntity" dance, and the PartyException handling. All of that used to
 * be copy pasted into every single subcommand.
 */
public abstract class AbstractPartyCommand extends Command {

    protected AbstractPartyCommand(String name, String... aliases) {
        super(name, aliases);
    }

    /** Returns the sender as a CustomPlayer, or messages them and returns null if they aren't one. */
    protected CustomPlayer requirePlayer(CommandSender sender) {
        if (sender instanceof CustomPlayer player) {
            return player;
        }
        sender.sendMessage("Only players can execute this command!");
        return null;
    }

    protected void sendError(CommandSender sender, String message) {
        sendError(sender, Component.text(message));
    }

    protected void sendError(CommandSender sender, Component component) {
        sender.sendMessage(component.color(NamedTextColor.RED));
        sender.playSound(Constants.ERROR_SOUND);
    }

    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
        sender.playSound(Constants.SUCCESS_SOUND);
    }

    protected void handlePartyException(CommandSender sender, PartyException e) {
        sendError(sender, e.getComponent());
    }

    /** Resolves an ArgumentEntity to a single CustomPlayer, or null if none/not a player. */
    protected CustomPlayer findSinglePlayer(CommandSender sender, ArgumentEntity arg, CommandContext context) {
        EntityFinder finder = context.get(arg);
        List<Entity> entities = finder.find(sender);

        if (entities.isEmpty() || !(entities.getFirst() instanceof CustomPlayer target)) {
            return null;
        }
        return target;
    }
}