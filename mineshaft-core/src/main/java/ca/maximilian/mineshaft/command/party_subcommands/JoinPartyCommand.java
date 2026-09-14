package ca.maximilian.mineshaft.command.party_subcommands;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import ca.maximilian.mineshaft.exeptions.AlreadyInPartyException;
import ca.maximilian.mineshaft.exeptions.NotInPartyException;
import ca.maximilian.mineshaft.exeptions.PartyException;
import ca.maximilian.mineshaft.lobby.Matchmaking;
import ca.maximilian.mineshaft.lobby.Party;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import org.jetbrains.annotations.Nullable;

public class JoinPartyCommand extends AbstractPartyCommand {

    private final ArgumentEntity playerArg = ArgumentType.Entity("player")
            .singleEntity(true)
            .onlyPlayers(true);
    private final ArgumentString passwordArgument = ArgumentType.String("password");

    public JoinPartyCommand() {
        super("join");

        setDefaultExecutor((sender, context) -> sendError(sender, "Usage /party join <player> [password]"));

        addSyntax(this::run, playerArg);
        addSyntax(this::run, playerArg, passwordArgument);
    }

    private void run(CommandSender sender, CommandContext context) {
        ExtractionPlayer player = requirePlayer(sender);
        if (player == null) return;

        try {
            if (Matchmaking.getPartyWithPlayer(player) != null) {
                throw new AlreadyInPartyException(Component.text("You are already in a party!"));
            }

            final @Nullable String password = context.get(passwordArgument);

            ExtractionPlayer targetPlayer = findSinglePlayer(sender, playerArg, context);
            if (targetPlayer == null) {
                sendError(sender, "Player not found!");
                return;
            }

            Party targetParty = Matchmaking.getPartyWithPlayer(targetPlayer);
            if (targetParty == null) {
                throw new NotInPartyException(Component.text("Target is not in a party!"));
            }

            targetParty.addPlayer(player, password);
        } catch (PartyException e) {
            handlePartyException(sender, e);
        }
    }
}