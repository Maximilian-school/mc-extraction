package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.AlreadyInPartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;

public class QuickplayPartyCommand extends Command {

    public QuickplayPartyCommand() {
        super("quickplay");

        setDefaultExecutor(this::run);
    }

    private void run(CommandSender sender, CommandContext context) {
        if (!(sender instanceof CustomPlayer player)) {
            sender.sendMessage("Only players can execute this command!");
            return;
        }

        try {
            Party party = Matchmaking.getLargestJoinableParty(player);

            if (party == null) {
                sender.sendMessage(Component.text("Could not find any joinable parties!").color(NamedTextColor.RED));
                sender.playSound(Constants.ERROR_SOUND);
                return;
            }

            party.addPlayer(player, null);
        } catch (AlreadyInPartyException alreadyInPartyException) {
            sender.sendMessage(AlreadyInPartyException.getComponent().color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        }
    }
}
