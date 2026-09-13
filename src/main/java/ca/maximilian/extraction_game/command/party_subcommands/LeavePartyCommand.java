package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.NotInPartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;

public class LeavePartyCommand extends AbstractPartyCommand {

    public LeavePartyCommand() {
        super("leave");
        setDefaultExecutor(this::run);
    }

    private void run(CommandSender sender, CommandContext context) {
        CustomPlayer player = requirePlayer(sender);
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