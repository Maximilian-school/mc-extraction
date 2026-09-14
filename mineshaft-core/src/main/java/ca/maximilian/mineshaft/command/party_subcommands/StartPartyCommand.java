package ca.maximilian.mineshaft.command.party_subcommands;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.exeptions.NotInPartyException;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;

public class StartPartyCommand extends AbstractPartyCommand {

    public StartPartyCommand() {
        super("start");
        setDefaultExecutor(this::run);
    }

    private void run(CommandSender sender, CommandContext context) {
        ExtractionPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) {
                throw new NotInPartyException(Component.text("You don't have a party!"));
            }
            if (party.getHost() != player) {
                sendError(sender, "You are not the host!");
                return;
            }
            if (party.isRunning()) {
                sendError(sender, "Game is running!");
                return;
            }
            party.start();
        } catch (NotInPartyException e) {
            handlePartyException(sender, e);
        }
    }
}