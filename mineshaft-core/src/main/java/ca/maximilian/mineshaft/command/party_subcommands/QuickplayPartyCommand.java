package ca.maximilian.mineshaft.command.party_subcommands;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.exeptions.AlreadyInPartyException;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;

public class QuickplayPartyCommand extends AbstractPartyCommand {

    public QuickplayPartyCommand() {
        super("quickplay");
        setDefaultExecutor(this::run);
    }

    private void run(CommandSender sender, CommandContext context) {
        ExtractionPlayer player = requirePlayer(sender);
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