package org.sawiq.chestdiff.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.config.ConfigRepository;
import org.sawiq.chestdiff.storage.HistoryStorage;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final ChestDiffConfig config;
    private final ConfigRepository configRepository;
    private final HistoryStorage storage;
    private final String currentScopeId;

    public SettingsScreen(
            Screen parent,
            ChestDiffConfig config,
            ConfigRepository configRepository,
            HistoryStorage storage,
            String currentScopeId
    ) {
        super(Component.translatable("chestdiff.screen.settings"));
        this.parent = parent;
        this.config = config;
        this.configRepository = configRepository;
        this.storage = storage;
        this.currentScopeId = currentScopeId;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = 38;
        addRenderableWidget(toggleButton(x, y, "chestdiff.setting.overlay", config::overlayEnabled, config::setOverlayEnabled));
        addRenderableWidget(toggleButton(x, y + 24, "chestdiff.setting.rearrangements",
                config::showRearrangements, config::setShowRearrangements));
        addRenderableWidget(toggleButton(x, y + 48, "chestdiff.setting.animations",
                config::animationsEnabled, config::setAnimationsEnabled));
        addRenderableWidget(toggleButton(x, y + 72, "chestdiff.setting.virtual",
                config::saveVirtualContainers, config::setSaveVirtualContainers));
        addRenderableWidget(toggleButton(x, y + 96, "chestdiff.setting.utilities",
                config::recordUtilityContainers, config::setRecordUtilityContainers));
        addRenderableWidget(toggleButton(x, y + 120, "chestdiff.setting.debug",
                config::debugLogging, config::setDebugLogging));

        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.delete_scope"), button ->
                        ScreenNavigationAdapter.open(minecraft, new ConfirmationScreen(
                                this,
                                Component.translatable("chestdiff.button.delete_scope"),
                                () -> storage.deleteScope(currentScopeId))))
                .bounds(x, y + 154, 220, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.delete_all"), button ->
                        ScreenNavigationAdapter.open(minecraft, new ConfirmationScreen(
                                this,
                                Component.translatable("chestdiff.button.delete_all"),
                                () -> storage.deleteAll())))
                .bounds(x, y + 178, 220, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.done"), button -> onClose())
                .bounds(width / 2 - 50, height - 27, 100, 20).build());
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        GuiRenderAdapter.centered(graphics, title, width / 2, 16, 0xFFFFFFFF);
    }
    //?} else {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiRenderAdapter.centered(graphics, title, width / 2, 16, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    @Override
    public void onClose() {
        configRepository.save(config);
        ScreenNavigationAdapter.open(minecraft, parent);
    }

    private Button toggleButton(
            int x,
            int y,
            String translationKey,
            BooleanValue getter,
            BooleanSetter setter
    ) {
        Button[] reference = new Button[1];
        reference[0] = Button.builder(toggleLabel(translationKey, getter.get()), button -> {
                    setter.set(!getter.get());
                    button.setMessage(toggleLabel(translationKey, getter.get()));
                    configRepository.save(config);
                })
                .bounds(x, y, 220, 20)
                .build();
        return reference[0];
    }

    private Component toggleLabel(String key, boolean enabled) {
        return Component.translatable(
                key,
                Component.translatable(enabled ? "chestdiff.state.on" : "chestdiff.state.off"));
    }

    @FunctionalInterface
    private interface BooleanValue {
        boolean get();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }
}
