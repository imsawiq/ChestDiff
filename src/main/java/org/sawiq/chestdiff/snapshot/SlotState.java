package org.sawiq.chestdiff.snapshot;

import java.util.Objects;

public record SlotState(int index, ItemFingerprint fingerprint, int count) {
    public SlotState {
        if (index < 0) {
            throw new IllegalArgumentException("Slot index cannot be negative");
        }
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (count <= 0) {
            throw new IllegalArgumentException("Stack count must be positive");
        }
    }
}
