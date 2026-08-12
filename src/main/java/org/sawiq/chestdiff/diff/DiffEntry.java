package org.sawiq.chestdiff.diff;

import org.sawiq.chestdiff.snapshot.ItemFingerprint;

import java.util.List;
import java.util.Objects;

public record DiffEntry(
        DiffType type,
        ItemFingerprint before,
        ItemFingerprint after,
        int count,
        List<Integer> beforeSlots,
        List<Integer> afterSlots
) {
    public DiffEntry {
        Objects.requireNonNull(type, "type");
        beforeSlots = List.copyOf(Objects.requireNonNullElse(beforeSlots, List.of()));
        afterSlots = List.copyOf(Objects.requireNonNullElse(afterSlots, List.of()));
        if (count <= 0) {
            throw new IllegalArgumentException("Diff count must be positive");
        }
        if (before == null && after == null) {
            throw new IllegalArgumentException("A diff entry needs an item");
        }
    }

    public ItemFingerprint visibleFingerprint() {
        return after != null ? after : before;
    }
}
