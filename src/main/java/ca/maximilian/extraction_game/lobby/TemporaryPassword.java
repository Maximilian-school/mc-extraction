package ca.maximilian.extraction_game.lobby;

import lombok.Getter;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public class TemporaryPassword {

    @Getter
    private final String password;

    @Getter
    private final long validSeconds;

    @Getter
    private final long validTill;

    private boolean invalidatedManually = false;

    public TemporaryPassword(String password, long validTill) {
        this.password = password;
        this.validTill = validTill;
        this.validSeconds = validTill - Instant.now().getEpochSecond();
    }

    public TemporaryPassword() {
        String password;
        try {
            SecureRandom random = new SecureRandom();
            byte[] randomBytes = new byte[32];
            random.nextBytes(randomBytes);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(randomBytes);

            password = HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            password = UUID.randomUUID().toString();
        }
        this.password = password;
        this.validTill = Instant.now().plusSeconds(30).getEpochSecond();
        this.validSeconds = 30;
    }

    public boolean isValid() {
        if (invalidatedManually) {
            return false;
        }
        return Instant.now().getEpochSecond() < this.validTill;
    }

    public void invalidate() {
        this.invalidatedManually = true;
    }
}