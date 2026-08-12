package org.sawiq.chestdiff.client.observation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.diff.SemanticDiffEngine;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.storage.ContainerHistory;
import org.sawiq.chestdiff.storage.HistoryStorage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ContainerObservationSession {
    private static final int STABLE_TICKS_REQUIRED = 3;

    private final AbstractContainerScreen<?> screen;
    private final ContainerIdentity identity;
    private final MenuSnapshotAdapter snapshotAdapter;
    private final SemanticDiffEngine diffEngine;
    private final HistoryStorage storage;
    private final Supplier<ChestDiffConfig> configSupplier;
    private final CompletableFuture<Optional<ContainerHistory>> loadedHistory;
    private ObservationView view;
    private long previousSignature;
    private int stableTicks;
    private boolean initialCaptured;
    private boolean closed;

    public ContainerObservationSession(
            AbstractContainerScreen<?> screen,
            ContainerIdentity identity,
            MenuSnapshotAdapter snapshotAdapter,
            SemanticDiffEngine diffEngine,
            HistoryStorage storage,
            Supplier<ChestDiffConfig> configSupplier
    ) {
        this.screen = screen;
        this.identity = identity;
        this.snapshotAdapter = snapshotAdapter;
        this.diffEngine = diffEngine;
        this.storage = storage;
        this.configSupplier = configSupplier;
        this.loadedHistory = identity.persistent()
                ? storage.load(identity)
                : CompletableFuture.completedFuture(Optional.empty());
        this.view = ObservationView.loading(identity);
    }

    public void tick(Minecraft client) {
        if (closed || initialCaptured || snapshotAdapter.containerSlotCount(screen.getMenu()) == 0) {
            return;
        }
        long signature = snapshotAdapter.synchronizationSignature(screen.getMenu());
        if (signature == previousSignature) {
            stableTicks++;
        } else {
            previousSignature = signature;
            stableTicks = 1;
        }
        if (stableTicks >= STABLE_TICKS_REQUIRED) {
            captureInitial(client);
        }
    }

    public void close(Minecraft client) {
        if (closed) {
            return;
        }
        closed = true;
        if (!initialCaptured || !identity.persistent()) {
            return;
        }
        ContainerSnapshot finalSnapshot = snapshotAdapter.capture(
                client, screen.getMenu(), screen.getTitle().getString());
        ChestDiffConfig config = configSupplier.get();
        Instant oldestAllowed = Instant.now().minus(config.retentionDays(), ChronoUnit.DAYS);
        loadedHistory.thenApply(history -> history.orElseGet(() -> ContainerHistory.empty(identity)))
                .thenApply(history -> history.withPinned(config.isPinned(identity.stableKey())))
                .thenApply(history -> history.append(finalSnapshot, config.snapshotsPerContainer(), oldestAllowed))
                .thenCompose(storage::save)
                .exceptionally(exception -> null);
    }

    public AbstractContainerScreen<?> screen() {
        return screen;
    }

    public ObservationView view() {
        return view;
    }

    private void captureInitial(Minecraft client) {
        initialCaptured = true;
        ContainerSnapshot current = snapshotAdapter.capture(client, screen.getMenu(), screen.getTitle().getString());
        loadedHistory.thenAccept(history -> client.execute(() -> {
            Optional<ContainerSnapshot> previous = history.flatMap(ContainerHistory::latestSnapshot);
            view = previous
                    .map(snapshot -> ObservationView.compared(
                            identity, snapshot, current, diffEngine.compare(snapshot, current)))
                    .orElseGet(() -> ObservationView.first(identity, current));
        })).exceptionally(exception -> {
            client.execute(() -> view = ObservationView.first(identity, current));
            return null;
        });
    }
}
