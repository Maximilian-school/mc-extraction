package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.NotInPartyException;
import ca.maximilian.extraction_game.exeptions.PartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;

public class KickPartyCommand extends AbstractPartyCommand {

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

        setDefaultExecutor((sender, context) -> sendError(sender, "Usage /party kick <player>"));

        addSyntax(this::run, playerArg);
    }

    private void run(CommandSender sender, CommandContext context) {
        CustomPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) {
                throw new NotInPartyException(Component.text("You are not in a party!"));
            }
            if (party.getHost() != player) {
                throw new NotInPartyException(Component.text("You are not the host!"));
            }

            CustomPlayer target = findSinglePlayer(sender, playerArg, context);
            if (target == null) {
                sendError(sender, "Player not found!");
                return;
            }
            if (target.equals(player)) {
                sendError(sender, "You cannot kick yourself!");
                return;
            }
            if (!party.getPlayers().contains(target)) {
                sendError(sender, "That player is not in your party!");
                return;
            }

            party.removePlayer(target, true);
            sendSuccess(sender, "Kicked " + target.getUsername() + " from the party.");
        } catch (PartyException e) {
            handlePartyException(sender, e);
        }
    }
}