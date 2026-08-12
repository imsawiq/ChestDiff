package org.sawiq.chestdiff.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.sawiq.chestdiff.ChestDiff;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.client.observation.ContainerObservationManager;
import org.sawiq.chestdiff.client.observation.ObservationView;
import org.sawiq.chestdiff.client.ui.ContainerOverlay;
import org.sawiq.chestdiff.client.ui.ContainerSnapshotHistoryScreen;
import org.sawiq.chestdiff.client.ui.DiffScreen;
import org.sawiq.chestdiff.client.ui.HistoryScreen;
import org.sawiq.chestdiff.client.ui.SettingsScreen;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.config.ConfigRepository;
import org.sawiq.chestdiff.storage.HistoryStorage;

public final class ChestDiffClient implements ClientModInitializer {
    private ConfigRepository configRepository;
    private ChestDiffConfig config;
    private HistoryStorage historyStorage;
    private ContainerObservationManager observations;
    private KeyMapping openHistoryKey;

    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        configRepository = new ConfigRepository(loader.getConfigDir());
        config = configRepository.load();
        historyStorage = new HistoryStorage(loader.getGameDir(), () -> config);
        observations = new ContainerObservationManager(historyStorage, () -> config);

        //? if >=1.21.11 {
        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(ChestDiff.MOD_ID, "controls"));
        openHistoryKey = registerKeyMapping(new KeyMapping(
                "key.chestdiff.open_history",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category));
        //?} elif >=1.21.9 {
        /*KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ChestDiff.MOD_ID, "controls"));
        openHistoryKey = registerKeyMapping(new KeyMapping(
                "key.chestdiff.open_history",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category));
        *///?} else {
        /*openHistoryKey = registerKeyMapping(new KeyMapping(
                "key.chestdiff.open_history",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.category.chestdiff"));
        *///?}

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            observations.tick(client);
            while (openHistoryKey.consumeClick()) {
                openCurrentDiffOrHistory(client);
            }
        });
        new ContainerOverlay(observations, () -> config).register();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> closeResources());
    }

    private void openCurrentDiffOrHistory(Minecraft client) {
        ObservationView active = observations.activeView().orElse(null);
        if (active != null && active.ready()) {
            if (client.player != null) {
                client.player.closeContainer();
            }
            ScreenNavigationAdapter.open(client, new ContainerSnapshotHistoryScreen(
                    null,
                    active,
                    observations.loadHistory(active.identity()),
                    () -> observations.deleteHistory(active.identity())));
            return;
        }
        HistoryScreen history = new HistoryScreen(
                ScreenNavigationAdapter.current(client),
                historyStorage,
                () -> ScreenNavigationAdapter.open(client, new SettingsScreen(
                        ScreenNavigationAdapter.current(client),
                        config,
                        configRepository,
                        historyStorage,
                        observations.currentWorldScope(client).id())));
        ScreenNavigationAdapter.open(client, history);
    }

    private void closeResources() {
        configRepository.save(config).join();
        configRepository.close();
        historyStorage.close();
    }

    private static KeyMapping registerKeyMapping(KeyMapping mapping) {
        //? if >=26.1
        return net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(mapping);
        //? if <26.1
        /*return net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(mapping);*/
    }
}
