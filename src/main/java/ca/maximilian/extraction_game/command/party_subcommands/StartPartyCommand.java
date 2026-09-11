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
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Entity;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

public class StartPartyCommand extends Command {

    public StartPartyCommand() {
        super("start");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof CustomPlayer player)) {
                sender.sendMessage("Only players can execute this command!");
                return;
            }

            try {
                Party party = Matchmaking.getPartyWithPlayer(player);

                if (party == null) throw new NotInPartyException(Component.text("You don't have a party!"));

                if (party.getHost() != player) {
                    sender.sendMessage(Component.text("You are not the host!"));
                    sender.playSound(Constants.ERROR_SOUND);
                    return;
                }

                party.start();
            } catch (NotInPartyException e) {
                sender.sendMessage(NotInPartyException.getComponent());
                sender.playSound(Constants.ERROR_SOUND);
            }
        });
    }
}