package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import ca.maximilian.extraction_game.exeptions.NotInPartyException;
import ca.maximilian.extraction_game.lobby.Matchmaking;
import ca.maximilian.extraction_game.lobby.Party;
import ca.maximilian.extraction_game.lobby.Invite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;

public class InvitePartyCommand extends AbstractPartyCommand {

    private final ArgumentEntity playerArg = ArgumentType.Entity("player")
            .singleEntity(true)
            .onlyPlayers(true);

    public InvitePartyCommand() {
        super("invite");

        setDefaultExecutor((sender, context) -> sendError(sender, "Usage /party invite <player>"));

        addSyntax(this::run, playerArg);
    }

    private void run(CommandSender sender, CommandContext context) {
        CustomPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            Party party = Matchmaking.getPartyWithPlayer(player);
            if (party == null) {
                throw new NotInPartyException(Component.text("You are not in a party!"));
            }

            CustomPlayer target = findSinglePlayer(sender, playerArg, context);
            if (target == null) {
                sendError(sender, "Player not found!");
                return;
            }

            if (target == player) {
                sendError(sender, "You cannot invite yourself!");
                return;
            }

            Invite invite = party.invitePlayer(target);

            target.sendMessage(
                    player.getName()
                            .append(Component.text(" has invited you to their party!\n"))
                            .append(Component.text("Click here to join")
                                    .decoration(TextDecoration.BOLD, true)
                                    .decoration(TextDecoration.UNDERLINED, true)
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand(
                                            "party join %s".formatted(party.getHost().getUsername())
                                    )))
                            .append(Component.text("\nThis invite will expire in %s seconds!".formatted(invite.getValidSeconds())))
            );
        } catch (NotInPartyException e) {
            handlePartyException(sender, e);
        }
    }
}