package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.AlreadyInPartyException;
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
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JoinPartyCommand extends Command {

    private final ArgumentEntity playerArg = ArgumentType.Entity("player")
            .singleEntity(true)
            .onlyPlayers(true);
    private final ArgumentString passwordArgument = ArgumentType.String("password");

    public JoinPartyCommand() {
        super("join");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage /party join <player> [password]").color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        });

        addSyntax(this::run, playerArg);
        addSyntax(this::run, playerArg, passwordArgument);
    }

    private void run(CommandSender sender, CommandContext context) {
        if (!(sender instanceof CustomPlayer player)) {
            sender.sendMessage("Only players can execute this command!");
            return;
        }

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party != null) {
                throw new AlreadyInPartyException(Component.text("You are already in a party!"));
            }

            final @Nullable String password = context.get(passwordArgument);

            final EntityFinder entityFinder = context.get(playerArg);
            List<Entity> entities = entityFinder.find(sender);

            CustomPlayer targetPlayer = null;

            for (Entity entity : entities) {
                if (entity instanceof CustomPlayer player1) {
                    targetPlayer = player1;
                    break;
                }
            }

            if (targetPlayer == null) {
                throw new IllegalStateException("Player not found!");
            }

            party = Matchmaking.getPartyWithPlayer(targetPlayer);

            if (party == null) {
                throw new NotInPartyException(Component.text("Target is not in a party!"));
            }

            Party.JoinStatus joinStatus = Matchmaking.getPartyWithPlayer(targetPlayer).addPlayer(player, password);
        } catch (AlreadyInPartyException notInPartyException) {
            sender.sendMessage(AlreadyInPartyException.getComponent().color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        } catch (NotInPartyException notInPartyException) {
            sender.sendMessage(NotInPartyException.getComponent().color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        }
    }
}
