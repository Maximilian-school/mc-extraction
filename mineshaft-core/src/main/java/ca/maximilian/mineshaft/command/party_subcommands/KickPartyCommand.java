package ca.maximilian.mineshaft.command.party_subcommands;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.exeptions.NotInPartyException;
import ca.maximilian.mineshaft.exeptions.PartyException;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
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
            if (!(sender instanceof ExtractionPlayer player)) return;

            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) return;

            for (ExtractionPlayer member : party.getPlayers()) {
                if (!member.equals(player)) {
                    suggestion.addEntry(new SuggestionEntry(member.getUsername()));
                }
            }
        });

        setDefaultExecutor((sender, context) -> sendError(sender, "Usage /party kick <player>"));

        addSyntax(this::run, playerArg);
    }

    private void run(CommandSender sender, CommandContext context) {
        ExtractionPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) {
                throw new NotInPartyException(Component.text("You are not in a party!"));
            }
            if (party.getHost() != player) {
                throw new NotInPartyException(Component.text("You are not the host!"));
            }

            ExtractionPlayer target = findSinglePlayer(sender, playerArg, context);
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