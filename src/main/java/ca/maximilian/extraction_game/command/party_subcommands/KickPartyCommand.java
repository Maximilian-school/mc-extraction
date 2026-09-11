package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.NotInPartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Entity;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

public class KickPartyCommand extends Command {

    private final ArgumentEntity playerArg = ArgumentType.Entity("player")
            .singleEntity(true)
            .onlyPlayers(true);

    public KickPartyCommand() {
        super("kick");

        // Filter tab completion to only show party members (excluding the command sender)
        playerArg.setSuggestionCallback((sender, context, suggestion) -> {
            if (!(sender instanceof CustomPlayer player)) return;

            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) return;

            for (CustomPlayer member : party.getPlayers()) {
                if (!member.equals(player)) {
                    suggestion.addEntry(new SuggestionEntry(member.getUsername()));
                }
            }
        });

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage /party kick <player>").color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        });

        addSyntax(this::run, playerArg);
    }

    private void run(CommandSender sender, CommandContext context) {
        if (!(sender instanceof CustomPlayer player)) {
            sender.sendMessage("Only players can execute this command!");
            return;
        }

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);

            if (party == null) {
                throw new NotInPartyException(Component.text("You are not in a party!"));
            }

            if (party.getHost() != player) {
                throw new NotInPartyException(Component.text("You are not the host!"));
            }

            // Retrieve the target entity from context
            EntityFinder finder = context.get(playerArg);
            List<Entity> entities = finder.find(sender);

            if (entities.isEmpty() || !(entities.get(0) instanceof CustomPlayer target)) {
                sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                sender.playSound(Constants.ERROR_SOUND);
                return;
            }

            if (target.equals(player)) {
                sender.sendMessage(Component.text("You cannot kick yourself!").color(NamedTextColor.RED));
                sender.playSound(Constants.ERROR_SOUND);
                return;
            }

            if (!party.getPlayers().contains(target)) {
                sender.sendMessage(Component.text("That player is not in your party!").color(NamedTextColor.RED));
                sender.playSound(Constants.ERROR_SOUND);
                return;
            }

            party.removePlayer(target, true);

            sender.sendMessage(Component.text("Kicked " + target.getUsername() + " from the party.").color(NamedTextColor.GREEN));
            sender.playSound(Constants.SUCCESS_SOUND);

        } catch (NotInPartyException e) {
            sender.sendMessage(e.getComponent().color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        }
    }
}