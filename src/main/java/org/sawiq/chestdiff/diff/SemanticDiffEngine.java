package org.sawiq.chestdiff.diff;

import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.snapshot.ItemFingerprint;
import org.sawiq.chestdiff.snapshot.SlotState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SemanticDiffEngine {
    public ContainerDiff compare(ContainerSnapshot previous, ContainerSnapshot current) {
        Map<ItemFingerprint, Integer> previousTotals = previous.aggregateTotals();
        Map<ItemFingerprint, Integer> currentTotals = current.aggregateTotals();
        List<DiffEntry> removed = new ArrayList<>();
        List<DiffEntry> added = new ArrayList<>();

        Set<ItemFingerprint> fingerprints = new HashSet<>(previousTotals.keySet());
        fingerprints.addAll(currentTotals.keySet());
        for (ItemFingerprint fingerprint : fingerprints) {
            int delta = currentTotals.getOrDefault(fingerprint, 0) - previousTotals.getOrDefault(fingerprint, 0);
            if (delta > 0) {
                added.add(new DiffEntry(
                        DiffType.ADDED, null, fingerprint, delta, List.of(), current.slotIndexes(fingerprint)));
            } else if (delta < 0) {
                removed.add(new DiffEntry(
                        DiffType.REMOVED, fingerprint, null, -delta, previous.slotIndexes(fingerprint), List.of()));
            }
        }

        List<DiffEntry> modified = pairConservativeModifications(removed, added);
        int rearrangedSlots = countRearrangedSlots(previous, current, fingerprints);

        List<DiffEntry> entries = new ArrayList<>(added.size() + removed.size() + modified.size());
        entries.addAll(added);
        entries.addAll(removed);
        entries.addAll(modified);
        entries.sort((left, right) -> {
            int typeOrder = left.type().compareTo(right.type());
            return typeOrder != 0
                    ? typeOrder
                    : left.visibleFingerprint().displayName().compareToIgnoreCase(right.visibleFingerprint().displayName());
        });

        return new ContainerDiff(
                previous.snapshotId(),
                current.snapshotId(),
                previous.capturedAt(),
                current.capturedAt(),
                entries,
                rearrangedSlots,
                List.of());
    }

    private List<DiffEntry> pairConservativeModifications(List<DiffEntry> removed, List<DiffEntry> added) {
        Map<String, List<DiffEntry>> removedByItem = groupByBaseItem(removed);
        Map<String, List<DiffEntry>> addedByItem = groupByBaseItem(added);
        List<DiffEntry> modified = new ArrayList<>();

        for (Map.Entry<String, List<DiffEntry>> group : removedByItem.entrySet()) {
            List<DiffEntry> oldCandidates = group.getValue();
            List<DiffEntry> newCandidates = addedByItem.getOrDefault(group.getKey(), List.of());
            if (oldCandidates.size() != 1 || newCandidates.size() != 1) {
                continue;
            }
            DiffEntry oldEntry = oldCandidates.getFirst();
            DiffEntry newEntry = newCandidates.getFirst();
            if (oldEntry.count() != newEntry.count() || !hasContinuityEvidence(oldEntry, newEntry)) {
                continue;
            }
            removed.remove(oldEntry);
            added.remove(newEntry);
            modified.add(new DiffEntry(
                    DiffType.MODIFIED,
                    oldEntry.before(),
                    newEntry.after(),
                    oldEntry.count(),
                    oldEntry.beforeSlots(),
                    newEntry.afterSlots()));
        }
        return modified;
    }

    private Map<String, List<DiffEntry>> groupByBaseItem(List<DiffEntry> entries) {
        Map<String, List<DiffEntry>> grouped = new LinkedHashMap<>();
        for (DiffEntry entry : entries) {
            grouped.computeIfAbsent(entry.visibleFingerprint().itemId(), ignored -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    private boolean hasContinuityEvidence(DiffEntry oldEntry, DiffEntry newEntry) {
        if (!oldEntry.before().hasSameBaseItem(newEntry.after())) {
            return false;
        }
        Set<Integer> oldSlots = new HashSet<>(oldEntry.beforeSlots());
        return newEntry.afterSlots().stream().anyMatch(oldSlots::contains)
                || (oldEntry.beforeSlots().size() == 1 && newEntry.afterSlots().size() == 1);
    }

    private int countRearrangedSlots(
            ContainerSnapshot previous,
            ContainerSnapshot current,
            Set<ItemFingerprint> fingerprints
    ) {
        Map<Integer, SlotState> previousSlots = previous.slotsByIndex();
        Map<Integer, SlotState> currentSlots = current.slotsByIndex();
        Set<Integer> changedSlots = new HashSet<>();

        for (ItemFingerprint fingerprint : fingerprints) {
            int stableQuantity = Math.min(
                    previous.aggregateTotals().getOrDefault(fingerprint, 0),
                    current.aggregateTotals().getOrDefault(fingerprint, 0));
            if (stableQuantity == 0) {
                continue;
            }
            List<Integer> before = previous.slotIndexes(fingerprint);
            List<Integer> after = current.slotIndexes(fingerprint);
            if (before.equals(after) && countsMatchAtSharedSlots(previousSlots, currentSlots, before)) {
                continue;
            }
            changedSlots.addAll(before);
            changedSlots.addAll(after);
        }
        return changedSlots.size();
    }

    private boolean countsMatchAtSharedSlots(
            Map<Integer, SlotState> previous,
            Map<Integer, SlotState> current,
            List<Integer> indexes
    ) {
        for (int index : indexes) {
            SlotState before = previous.get(index);
            SlotState after = current.get(index);
            if (before == null || after == null || before.count() != after.count()) {
                return false;
            }
        }
        return true;
    }
}
