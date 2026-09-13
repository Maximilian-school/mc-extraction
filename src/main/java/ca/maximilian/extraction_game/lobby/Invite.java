package ca.maximilian.extraction_game.lobby;

import ca.maximilian.extraction_game.core.handler.CustomPlayer;
import lombok.Getter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class Invite {

    @Getter
    private final CustomPlayer player;

    @Getter
    private final long validSeconds;

    @Getter
    private final long validTill;

    private boolean used = false;

    public Invite(CustomPlayer player) {
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