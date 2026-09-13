package ca.maximilian.extraction_game.lobby;

import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import lombok.Getter;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Matchmaking {

    @Getter
    private static final List<Party> parties = new ArrayList<Party>();

    public static void tickParties() {
        for (Party party : parties) {
            party.tickParty();
        }
    }

    public static Party getLargestJoinableParty(CustomPlayer player) {
        Party largestPublicParty = null;

        for (Party party : parties) {
            if (!party.isPublic()) continue;

            if (party.isRunning()) continue;

            if (party.getKickedPlayers().contains(player)) {
                continue;
            }

            if (party.isFull()) continue;

            if (party.getPlayers().size() >= (largestPublicParty != null ? largestPublicParty.getPlayers().size() : 0)) {
                largestPublicParty = party;
            }
        }

        return largestPublicParty;
    }

    public static void addParty(Party party) {
        parties.add(party);
    }

    public static void removeParty(Party party) {
        parties.remove(party);
    }

    public static Party getPartyWithPlayer(Player player) {
        for (Party party : parties) {
            if (party.getPlayers().contains((CustomPlayer) player)) {
                return party;
            }
        }

        return null;
    }
}