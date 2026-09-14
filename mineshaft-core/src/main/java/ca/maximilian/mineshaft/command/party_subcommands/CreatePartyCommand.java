package ca.maximilian.mineshaft.command.party_subcommands;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class CreatePartyCommand extends AbstractPartyCommand {

    private static final int MAX_PARTY_SIZE = 8;

    private final Argument<Integer> maximumPlayersArgument = ArgumentType.Integer("maximum players").setDefaultValue(4);
    private final ArgumentString passwordArgument = ArgumentType.String("password");

    public CreatePartyCommand() {
        super("create");

        setDefaultExecutor(this::run);
        addSyntax(this::run, maximumPlayersArgument);
        addSyntax(this::run, maximumPlayersArgument, passwordArgument);
    }

    private void run(CommandSender sender, CommandContext context) {
        ExtractionPlayer player = requirePlayer(sender);
        if (player == null) return;

        final int maxPlayers = context.get(maximumPlayersArgument);
        if (maxPlayers <= 0) {
            sendError(sender, "The maximum players must be greater than 0!");
            return; // the old version just kept going after this, oops
        }
        if (maxPlayers > MAX_PARTY_SIZE) {
            sendError(sender, "The maximum players must be " + MAX_PARTY_SIZE + " or fewer!");
            return;
        }

        final String password = context.get(passwordArgument);

        try {
            Party party = new Party(player, password, maxPlayers);
            Matchmaking.addParty(party);
            sendSuccess(sender, "Successfully created party!");
        } catch (IllegalStateException e) {
            sendError(sender, e.getMessage());
        }
    }
}