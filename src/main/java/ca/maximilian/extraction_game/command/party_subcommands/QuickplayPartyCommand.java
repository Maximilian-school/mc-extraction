package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.AlreadyInPartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;

public class QuickplayPartyCommand extends AbstractPartyCommand {

    public QuickplayPartyCommand() {
        super("quickplay");
        setDefaultExecutor(this::run);
    }

    private void run(CommandSender sender, CommandContext context) {
        CustomPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            Party party = Matchmaking.getLargestJoinableParty(player);
            if (party == null) {
                sendError(sender, "Could not find any joinable parties!");
                return;
            }
            party.addPlayer(player, null);
        } catch (AlreadyInPartyException e) {
            handlePartyException(sender, e);
        }
    }
}