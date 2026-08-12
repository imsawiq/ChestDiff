package org.sawiq.chestdiff.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public record ContainerIdentity(
        WorldScope worldScope,
        IdentityType type,
        ContainerKind kind,
        String locator,
        int epoch,
        String displayName,
        String dimension,
        List<String> positions,
        boolean persistent
) {
    public ContainerIdentity {
        Objects.requireNonNull(worldScope, "worldScope");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(locator, "locator");
        displayName = Objects.requireNonNullElse(displayName, kind.name());
        dimension = Objects.requireNonNullElse(dimension, "");
        positions = List.copyOf(Objects.requireNonNullElse(positions, List.of()));
        if (epoch < 0) {
            throw new IllegalArgumentException("Container epoch cannot be negative");
        }
    }

    public String stableKey() {
        return worldScope.id() + '|' + type + '|' + kind + '|' + locator + '|' + epoch;
    }

    public String fileHash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(stableKey().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
