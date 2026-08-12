package org.sawiq.chestdiff.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;

public final class ConfirmationScreen extends Screen {
    private final Screen parent;
    private final Component message;
    private final Runnable confirmed;

    public ConfirmationScreen(Screen parent, Component message, Runnable confirmed) {
        super(Component.translatable("chestdiff.button.confirm"));
        this.parent = parent;
        this.message = message;
        this.confirmed = confirmed;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.confirm"), button -> {
                    confirmed.run();
                    ScreenNavigationAdapter.open(minecraft, parent);
                })
                .bounds(width / 2 - 105, height / 2 + 20, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.cancel"), button -> onClose())
                .bounds(width / 2 + 5, height / 2 + 20, 100, 20).build());
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        GuiRenderAdapter.centered(graphics, title, width / 2, height / 2 - 28, 0xFFFF6476);
        GuiRenderAdapter.centered(graphics, message, width / 2, height / 2 - 8, 0xFFE7EAF0);
    }
    //?} else {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiRenderAdapter.centered(graphics, title, width / 2, height / 2 - 28, 0xFFFF6476);
        GuiRenderAdapter.centered(graphics, message, width / 2, height / 2 - 8, 0xFFE7EAF0);
    }
    *///?}

    @Override
    public void onClose() {
        ScreenNavigationAdapter.open(minecraft, parent);
    }
}
