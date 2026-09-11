package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.NotInPartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import ca.maximilian.extraction_game.lobby.TemporaryPassword;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Entity;
import net.minestom.server.utils.entity.EntityFinder;

import javax.management.openmbean.CompositeType;
import java.util.List;

public class InvitePartyCommand extends Command {

    private final ArgumentEntity playerArg = ArgumentType.Entity("player")
            .singleEntity(true)
            .onlyPlayers(true);

    public InvitePartyCommand() {
        super("invite");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage /party kick <player>").color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        });

        addSyntax(this::run, playerArg);
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

            EntityFinder finder = context.get(playerArg);
            List<Entity> entities = finder.find(sender);

            if (entities.isEmpty() || !(entities.getFirst() instanceof CustomPlayer target)) {
                sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                sender.playSound(Constants.ERROR_SOUND);
                return;
            }

            TemporaryPassword temporaryPassword = party.addTemporaryPassword();

            target.sendMessage(
                    player.getName().append(Component.text(" has invited you to their party!\n")).append(
                            Component.text("Click here to join")
                                    .decoration(TextDecoration.BOLD, true)
                                    .decoration(TextDecoration.UNDERLINED, true)
                                    .color(NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand(
                                            "party join %s %s".formatted(
                                                    party.getHost().getUsername(),
                                                    temporaryPassword.getPassword()
                                            )
                                    ))
                    ).append(Component.text("\n This invite will invalidate in %s seconds!".formatted(temporaryPassword.getValidSeconds()))
            ));
        } catch (NotInPartyException e) {
            sender.sendMessage(NotInPartyException.getComponent().color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        }
    }
}