package ca.maximilian.mineshaft.lobby;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import lombok.Getter;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Matchmaking {

    @Getter
    private static final List<Party> parties = new ArrayList<Party>();

    public static void tickParties() {
        // Create a copy of the list to avoid ConcurrentModificationException
        // when parties remove themselves during tickParty()
        List<Party> partiesCopy = new ArrayList<>(parties);
        for (Party party : partiesCopy) {
            party.tickParty();
        }
    }

    public static Party getLargestJoinableParty(ExtractionPlayer player) {
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
            if (party.getPlayers().contains((ExtractionPlayer) player)) {
                return party;
            }
        }

        return null;
    }
}