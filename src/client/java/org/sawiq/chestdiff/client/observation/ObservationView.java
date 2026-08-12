package org.sawiq.chestdiff.client.observation;

import org.sawiq.chestdiff.diff.ContainerDiff;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;

import java.util.Optional;

public record ObservationView(
        ContainerIdentity identity,
        ContainerSnapshot previous,
        ContainerSnapshot current,
        ContainerDiff diff,
        boolean firstObservation,
        boolean ready,
        long readyAtEpochMilli
) {
    public static ObservationView loading(ContainerIdentity identity) {
        return new ObservationView(identity, null, null, null, false, false, 0L);
    }

    public static ObservationView first(ContainerIdentity identity, ContainerSnapshot current) {
        return new ObservationView(identity, null, current, null, true, true, System.currentTimeMillis());
    }

    public static ObservationView compared(
            ContainerIdentity identity,
            ContainerSnapshot previous,
            ContainerSnapshot current,
            ContainerDiff diff
    ) {
        return new ObservationView(identity, previous, current, diff, false, true, System.currentTimeMillis());
    }

    public Optional<ContainerDiff> diffOptional() {
        return Optional.ofNullable(diff);
    }
}
