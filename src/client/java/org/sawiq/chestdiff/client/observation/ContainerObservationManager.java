package org.sawiq.chestdiff.client.observation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.client.identity.ContainerIdentityResolver;
import org.sawiq.chestdiff.client.identity.InteractionCorrelation;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.diff.SemanticDiffEngine;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.identity.WorldScope;
import org.sawiq.chestdiff.storage.HistoryStorage;
import org.sawiq.chestdiff.storage.ContainerHistory;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;

public final class ContainerObservationManager {
    private final InteractionCorrelation correlation = new InteractionCorrelation();
    private final ContainerIdentityResolver identityResolver = new ContainerIdentityResolver();
    private final MenuSnapshotAdapter snapshotAdapter = new MenuSnapshotAdapter();
    private final SemanticDiffEngine diffEngine = new SemanticDiffEngine();
    private final HistoryStorage storage;
    private final Supplier<ChestDiffConfig> configSupplier;
    private ContainerObservationSession activeSession;

    public ContainerObservationManager(HistoryStorage storage, Supplier<ChestDiffConfig> configSupplier) {
        this.storage = storage;
        this.configSupplier = configSupplier;
    }

    public void tick(Minecraft client) {
        correlation.tick(client);
        identityResolver.tickKnownBlocks(client);
        AbstractContainerScreen<?> currentScreen = observedScreen(ScreenNavigationAdapter.current(client)).orElse(null);

        if (activeSession != null && activeSession.screen() != currentScreen) {
            activeSession.close(client);
            activeSession = null;
        }
        if (currentScreen != null && activeSession == null) {
            startSession(client, currentScreen);
        }
        if (activeSession != null) {
            activeSession.tick(client);
        }
    }

    public Optional<ObservationView> activeView(Screen screen) {
        if (activeSession == null || activeSession.screen() != screen) {
            return Optional.empty();
        }
        return Optional.of(activeSession.view());
    }

    public Optional<ObservationView> activeView() {
        return activeSession == null ? Optional.empty() : Optional.of(activeSession.view());
    }

    public WorldScope currentWorldScope(Minecraft client) {
        return identityResolver.resolveScope(client);
    }

    public void deleteHistory(ContainerIdentity identity) {
        storage.delete(identity);
    }

    public CompletableFuture<Optional<ContainerHistory>> loadHistory(ContainerIdentity identity) {
        return storage.load(identity);
    }

    private void startSession(Minecraft client, AbstractContainerScreen<?> screen) {
        ChestDiffConfig config = configSupplier.get();
        Optional<InteractionCorrelation.CorrelatedTarget> target = correlation.consume(client);
        if (!SupportedContainerMenus.shouldObserve(screen.getMenu(), config.recordUtilityContainers())) {
            return;
        }
        if (identityResolver.isEnderStorage(client, target, screen.getTitle())) {
            return;
        }
        String menuType = screen.getMenu().getClass().getName();
        ContainerIdentity identity = identityResolver.resolve(
                client, target, menuType, screen.getTitle().getString(), config);
        activeSession = new ContainerObservationSession(
                screen, identity, snapshotAdapter, diffEngine, storage, configSupplier);
    }

    private Optional<AbstractContainerScreen<?>> observedScreen(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen) {
            return Optional.empty();
        }
        return Optional.of(containerScreen);
    }
}
