package org.sawiq.chestdiff.storage;

import org.sawiq.chestdiff.ChestDiff;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ContainerHistory(
        int schemaVersion,
        ContainerIdentity identity,
        boolean pinned,
        Instant lastObservedAt,
        List<ContainerSnapshot> snapshots
) {
    private static final int MINIMUM_PINNED_SNAPSHOT_LIMIT = 100;

    public ContainerHistory {
        Objects.requireNonNull(identity, "identity");
        lastObservedAt = Objects.requireNonNullElse(lastObservedAt, Instant.EPOCH);
        snapshots = List.copyOf(Objects.requireNonNullElse(snapshots, List.of()));
    }

    public static ContainerHistory empty(ContainerIdentity identity) {
        return new ContainerHistory(ChestDiff.DATA_SCHEMA_VERSION, identity, false, Instant.EPOCH, List.of());
    }

    public Optional<ContainerSnapshot> latestSnapshot() {
        return snapshots.isEmpty() ? Optional.empty() : Optional.of(snapshots.getLast());
    }

    public ContainerHistory append(ContainerSnapshot snapshot, int maximumSnapshots, Instant oldestAllowed) {
        List<ContainerSnapshot> retained = new ArrayList<>(snapshots.size() + 1);
        for (ContainerSnapshot existing : snapshots) {
            if (pinned || !existing.capturedAt().isBefore(oldestAllowed)) {
                appendIfContentsChanged(retained, existing);
            }
        }
        appendIfContentsChanged(retained, snapshot);
        int snapshotLimit = pinned
                ? Math.max(MINIMUM_PINNED_SNAPSHOT_LIMIT, maximumSnapshots)
                : Math.max(1, maximumSnapshots);
        int overflow = retained.size() - snapshotLimit;
        if (overflow > 0) {
            retained = new ArrayList<>(retained.subList(overflow, retained.size()));
        }
        return new ContainerHistory(
                ChestDiff.DATA_SCHEMA_VERSION,
                identity,
                pinned,
                snapshot.capturedAt(),
                retained);
    }

    private static void appendIfContentsChanged(
            List<ContainerSnapshot> snapshots,
            ContainerSnapshot candidate
    ) {
        if (snapshots.isEmpty() || !snapshots.getLast().hasSameContents(candidate)) {
            snapshots.add(candidate);
        }
    }

    public ContainerHistory withPinned(boolean isPinned) {
        return new ContainerHistory(schemaVersion, identity, isPinned, lastObservedAt, snapshots);
    }
}
