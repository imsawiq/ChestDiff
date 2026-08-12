package org.sawiq.chestdiff.snapshot;

import java.util.Objects;

public record ItemFingerprint(
        String hash,
        String itemId,
        String displayName,
        String canonicalComponents,
        int damage
) {
    public ItemFingerprint {
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(itemId, "itemId");
        displayName = Objects.requireNonNullElse(displayName, itemId);
        canonicalComponents = Objects.requireNonNullElse(canonicalComponents, "{}");
    }

    public boolean hasSameBaseItem(ItemFingerprint other) {
        return other != null && itemId.equals(other.itemId);
    }
}
