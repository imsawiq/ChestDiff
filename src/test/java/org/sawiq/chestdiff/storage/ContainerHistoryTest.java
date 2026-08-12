package org.sawiq.chestdiff.storage;

import org.junit.jupiter.api.Test;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.identity.ContainerKind;
import org.sawiq.chestdiff.identity.IdentityType;
import org.sawiq.chestdiff.identity.WorldScope;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.snapshot.ItemFingerprint;
import org.sawiq.chestdiff.snapshot.SlotState;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ContainerHistoryTest {
    @Test
    void pinnedHistoryRetainsMoreSnapshotsButRemainsBounded() {
        ContainerHistory history = ContainerHistory.empty(identity()).withPinned(true);
        for (int index = 0; index < 150; index++) {
            history = history.append(snapshot(index, index + 1), 20, Instant.EPOCH);
        }

        assertEquals(100, history.snapshots().size());
        assertEquals(50L, history.snapshots().getFirst().capturedAt().getEpochSecond());
        assertEquals(149L, history.snapshots().getLast().capturedAt().getEpochSecond());
    }

    @Test
    void identicalConsecutiveObservationsUpdateTimeWithoutAddingSnapshots() {
        ContainerHistory history = ContainerHistory.empty(identity())
                .append(snapshot(10, 4), 20, Instant.EPOCH)
                .append(snapshot(20, 4), 20, Instant.EPOCH)
                .append(snapshot(30, 4), 20, Instant.EPOCH);

        assertEquals(1, history.snapshots().size());
        assertEquals(10L, history.snapshots().getFirst().capturedAt().getEpochSecond());
        assertEquals(30L, history.lastObservedAt().getEpochSecond());
    }

    @Test
    void changedContentsCreateANewSnapshot() {
        ContainerHistory history = ContainerHistory.empty(identity())
                .append(snapshot(10, 4), 20, Instant.EPOCH)
                .append(snapshot(20, 5), 20, Instant.EPOCH);

        assertEquals(2, history.snapshots().size());
        assertEquals(5, history.snapshots().getLast().slots().getFirst().count());
    }

    @Test
    void existingConsecutiveDuplicatesAreCompactedOnNextObservation() {
        ContainerSnapshot first = snapshot(10, 4);
        ContainerSnapshot duplicate = snapshot(20, 4);
        ContainerHistory legacy = new ContainerHistory(
                2, identity(), false, duplicate.capturedAt(), List.of(first, duplicate));

        ContainerHistory compacted = legacy.append(snapshot(30, 4), 20, Instant.EPOCH);

        assertEquals(1, compacted.snapshots().size());
        assertEquals(30L, compacted.lastObservedAt().getEpochSecond());
    }

    private static ContainerIdentity identity() {
        return new ContainerIdentity(
                new WorldScope("scope", "Test", true),
                IdentityType.BLOCK,
                ContainerKind.CHEST,
                "minecraft:overworld:0,64,0",
                0,
                "Chest",
                "minecraft:overworld",
                List.of("0,64,0"),
                true);
    }

    private static ContainerSnapshot snapshot(long second, int count) {
        ItemFingerprint fingerprint = new ItemFingerprint(
                "stone", "minecraft:stone", "Stone", "{}", 0);
        return new ContainerSnapshot(
                "snapshot-" + second,
                Instant.ofEpochSecond(second),
                second,
                "Chest",
                27,
                List.of(new SlotState(0, fingerprint, count)));
    }
}
