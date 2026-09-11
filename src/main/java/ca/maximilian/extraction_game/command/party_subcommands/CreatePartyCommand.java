package ca.maximilian.extraction_game.command.party_subcommands;

import ca.maximilian.extraction_game.Constants;
import ca.maximilian.extraction_game.core.handler.CustomPlayer;
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

public class CreatePartyCommand extends Command {

    private final Argument<java.lang.Integer> maximumPlayersArgument = ArgumentType.Integer("maximum players").setDefaultValue(4);
    private final ArgumentString passwordArgument = ArgumentType.String("password");

    public CreatePartyCommand() {
        super("create");

        setDefaultExecutor(this::run);
        addSyntax(this::run, this.maximumPlayersArgument);
        addSyntax(this::run, this.maximumPlayersArgument, this.passwordArgument);
    }

    private void run(CommandSender sender, CommandContext context) {
        if (!(sender instanceof CustomPlayer player)) {
            sender.sendMessage("Only players can execute this command!");
            return;
        }

        final int maxPlayers = context.get(maximumPlayersArgument);
        if (maxPlayers <= 0) {
            sender.sendMessage("The maximum players must be greater than 0!");
        } else if (maxPlayers > 8) {
            sender.sendMessage("The maximum players must be less than 8!");
        }

        final String password = context.get(passwordArgument);

        try {
            Party party = new Party(player, password, maxPlayers);
            Matchmaking.addParty(party);

            sender.sendMessage(Component.text("Successfully created party!").color(NamedTextColor.GREEN));
            sender.playSound(Constants.SUCCESS_SOUND);
        } catch (IllegalStateException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
            sender.playSound(Constants.ERROR_SOUND);
        }
    }
}
