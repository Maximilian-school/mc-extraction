package ca.maximilian.extraction_game.command;

import ca.maximilian.extraction_game.command.party_subcommands.*;
import net.minestom.server.command.builder.Command;

public class PartyCommand extends Command {

    public PartyCommand() {
        super("party", "lobby", "lb", "room");

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage("Usage: /party <action>");
        });

        addSubcommands(
                new CreatePartyCommand(),
                new KickPartyCommand(),
                new StartPartyCommand(),
                new InvitePartyCommand(),

                new JoinPartyCommand(),
                new LeavePartyCommand(),
                new QuickplayPartyCommand()
        );
    }
}
