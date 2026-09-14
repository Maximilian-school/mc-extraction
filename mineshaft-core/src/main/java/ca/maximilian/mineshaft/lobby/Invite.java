package ca.maximilian.mineshaft.lobby;

import ca.maximilian.mineshaft.core.ExtractionPlayer;
import lombok.Getter;

import java.time.Instant;

public class Invite {

    @Getter
    private final ExtractionPlayer player;

    @Getter
    private final long validSeconds;

    @Getter
    private final long validTill;

    private boolean used = false;

    public Invite(ExtractionPlayer player) {
        this.player = player;
        this.validTill = Instant.now().plusSeconds(30).getEpochSecond();
        this.validSeconds = 30;
    }

    public boolean expired() {
        return Instant.now().getEpochSecond() < this.validTill;
    }

    public boolean used() {
        return used;
    }

    public boolean isValid() {
        if (used) {
            return false;
        }
        return Instant.now().getEpochSecond() < this.validTill;
    }

    public void use() {
        this.used = true;
    }
}