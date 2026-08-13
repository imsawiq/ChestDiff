package org.sawiq.chestdiff.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.storage.HistoryStorage;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final HistoryStorage storage;
    private final String currentScopeId;

    public SettingsScreen(
            Screen parent,
            HistoryStorage storage,
            String currentScopeId
    ) {
        super(Component.translatable("chestdiff.screen.settings"));
        this.parent = parent;
        this.storage = storage;
        this.currentScopeId = currentScopeId;
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        int y = height / 2 - 24;

        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.delete_scope"), button ->
                        ScreenNavigationAdapter.open(minecraft, new ConfirmationScreen(
                                this,
                                Component.translatable("chestdiff.button.delete_scope"),
                                () -> storage.deleteScope(currentScopeId))))
                .bounds(x, y, 220, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.delete_all"), button ->
                        ScreenNavigationAdapter.open(minecraft, new ConfirmationScreen(
                                this,
                                Component.translatable("chestdiff.button.delete_all"),
                                () -> storage.deleteAll())))
                .bounds(x, y + 24, 220, 20).build());
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
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiRenderAdapter.centered(graphics, title, width / 2, 16, 0xFFFFFFFF);
    }
    *///?}

    @Override
    public void onClose() {
        ScreenNavigationAdapter.open(minecraft, parent);
    }
}
