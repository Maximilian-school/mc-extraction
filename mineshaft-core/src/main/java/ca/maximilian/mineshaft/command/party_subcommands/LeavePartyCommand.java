package ca.maximilian.mineshaft.command.party_subcommands;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.exeptions.NotInPartyException;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;

public class LeavePartyCommand extends AbstractPartyCommand {

    public LeavePartyCommand() {
        super("leave");
        setDefaultExecutor(this::run);
    }

    private void run(CommandSender sender, CommandContext context) {
        ExtractionPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) {
                throw new NotInPartyException(Component.text("You are not in a party!"));
            }
            boolean left = party.removePlayer(player, false);
            if (left) {
                sendSuccess(sender, "Left party!");
            }
        } catch (NotInPartyException e) {
            handlePartyException(sender, e);
        }
    }
}