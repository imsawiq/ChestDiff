package org.sawiq.chestdiff.diff;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ContainerDiff(
        String previousSnapshotId,
        String currentSnapshotId,
        Instant previousObservedAt,
        Instant currentObservedAt,
        List<DiffEntry> entries,
        int rearrangedSlots,
        List<String> warnings
) {
    public ContainerDiff {
        Objects.requireNonNull(previousSnapshotId, "previousSnapshotId");
        Objects.requireNonNull(currentSnapshotId, "currentSnapshotId");
        Objects.requireNonNull(previousObservedAt, "previousObservedAt");
        Objects.requireNonNull(currentObservedAt, "currentObservedAt");
        entries = List.copyOf(Objects.requireNonNullElse(entries, List.of()));
        warnings = List.copyOf(Objects.requireNonNullElse(warnings, List.of()));
    }

    public boolean hasChanges() {
        return !entries.isEmpty() || rearrangedSlots > 0;
    }

    public int semanticChangeCount() {
        return (int) entries.stream().filter(entry -> entry.type() != DiffType.REARRANGED).count()
                + (rearrangedSlots > 0 ? 1 : 0);
    }

    public List<DiffEntry> entriesOfType(DiffType type) {
        return entries.stream().filter(entry -> entry.type() == type).toList();
    }
}
