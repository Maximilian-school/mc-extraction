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
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class LeavePartyCommand extends Command {

    public LeavePartyCommand() {
        super("leave");

        setDefaultExecutor(this::run);
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
            party.removePlayer(player, false);

            sender.sendMessage(Component.text("Left party!").color(NamedTextColor.RED));
            sender.playSound(Constants.SUCCESS_SOUND);
        } catch (NotInPartyException notInPartyException) {
            sender.sendMessage(NotInPartyException.getComponent().color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        }
    }
}
