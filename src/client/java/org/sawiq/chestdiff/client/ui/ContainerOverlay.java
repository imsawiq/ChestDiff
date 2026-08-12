package org.sawiq.chestdiff.client.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.client.observation.ContainerObservationManager;
import org.sawiq.chestdiff.client.observation.ObservationView;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.diff.DiffEntry;
import org.sawiq.chestdiff.diff.DiffType;
import org.sawiq.chestdiff.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ContainerOverlay {
    private static final int ICON_SIZE = 16;
    private static final int ADDED_COLOR = 0xE048D67A;
    private static final int REMOVED_COLOR = 0xE0F05261;
    private static final int MODIFIED_COLOR = 0xE0589DFF;

    private final ContainerObservationManager observations;
    private final Supplier<ChestDiffConfig> configSupplier;

    public ContainerOverlay(ContainerObservationManager observations, Supplier<ChestDiffConfig> configSupplier) {
        this.observations = observations;
        this.configSupplier = configSupplier;
    }

    public void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }
            OverlayState state = new OverlayState();

            //? if >=1.21.9 {
            ScreenMouseEvents.allowMouseClick(screen).register((rendered, event) ->
                    !handleClick(client, containerScreen, state, event.x(), event.y(), event.button()));
            //?} else {
            /*ScreenMouseEvents.allowMouseClick(screen).register((rendered, mouseX, mouseY, button) ->
                    !handleClick(client, containerScreen, state, mouseX, mouseY, button));
            *///?}

            //? if >=26.1 {
            ScreenEvents.beforeExtract(screen).register((rendered, graphics, mouseX, mouseY, tickDelta) ->
                    prepareTooltip(containerScreen, graphics, mouseX, mouseY, state));
            ScreenEvents.afterBackground(screen).register((rendered, graphics, mouseX, mouseY, tickDelta) ->
                    render(containerScreen, graphics, mouseX, mouseY, state, false));
            //?} elif >=1.21.6 {
            /*ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, tickDelta) -> {
                render(containerScreen, graphics, mouseX, mouseY, state, false);
                prepareTooltip(containerScreen, graphics, mouseX, mouseY, state);
                GuiRenderAdapter.renderPendingTooltip(graphics);
            });
            *///?} else {
            /*ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, tickDelta) ->
                    render(containerScreen, graphics, mouseX, mouseY, state, true));
            *///?}
        });
    }

    private void render(
            AbstractContainerScreen<?> screen,
            Object graphics,
            int mouseX,
            int mouseY,
            OverlayState state,
            boolean renderTooltip
    ) {
        ObservationView view = observations.activeView(screen).orElse(null);
        if (view == null || !view.ready() || !configSupplier.get().overlayEnabled()) {
            return;
        }

        IconPosition icon = iconPosition(screen);
        boolean hovered = icon.contains(mouseX, mouseY);
        int iconColor = hovered ? 0xFFFFFFFF : 0xFFD8D8D8;
        GuiRenderAdapter.text(graphics, Component.literal("⌛"), icon.x() + 3, icon.y() + 3, iconColor, true);

        boolean hasChanges = view.diff() != null && view.diff().hasChanges();
        if (hasChanges) {
            drawNotificationDot(graphics, icon.x() + 11, icon.y() - 2);
        }
        if (state.highlightsVisible && hasChanges) {
            drawSlotChanges(screen, graphics, view, mouseX, mouseY, renderTooltip);
        }
        if (hovered && renderTooltip) {
            GuiRenderAdapter.tooltip(graphics, iconTooltip(hasChanges, state.highlightsVisible), mouseX, mouseY);
        }
    }

    private void prepareTooltip(
            AbstractContainerScreen<?> screen,
            Object graphics,
            int mouseX,
            int mouseY,
            OverlayState state
    ) {
        ObservationView view = observations.activeView(screen).orElse(null);
        if (view == null || !view.ready() || !configSupplier.get().overlayEnabled()) {
            return;
        }
        boolean hasChanges = view.diff() != null && view.diff().hasChanges();
        if (iconPosition(screen).contains(mouseX, mouseY)) {
            GuiRenderAdapter.tooltip(graphics, iconTooltip(hasChanges, state.highlightsVisible), mouseX, mouseY);
            return;
        }
        if (state.highlightsVisible && hasChanges) {
            SlotChange hoveredChange = hoveredSlotChange(screen, view, mouseX, mouseY);
            if (hoveredChange != null) {
                GuiRenderAdapter.tooltip(graphics, hoveredChange.tooltip(), mouseX, mouseY);
            }
        }
    }

    private List<Component> iconTooltip(boolean hasChanges, boolean highlightsVisible) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("chestdiff.tooltip.icon.title").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(hasChanges
                ? "chestdiff.tooltip.icon.changed"
                : "chestdiff.tooltip.icon.unchanged").withStyle(
                hasChanges ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (hasChanges) {
            tooltip.add(Component.translatable(highlightsVisible
                    ? "chestdiff.tooltip.icon.click_hide"
                    : "chestdiff.tooltip.icon.click").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("chestdiff.tooltip.icon.shift_click")
                .withStyle(ChatFormatting.AQUA));
        return List.copyOf(tooltip);
    }

    private boolean handleClick(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            OverlayState state,
            double mouseX,
            double mouseY,
            int button
    ) {
        ObservationView view = observations.activeView(screen).orElse(null);
        if (button != 0 || view == null || !view.ready() || !configSupplier.get().overlayEnabled()
                || !iconPosition(screen).contains(mouseX, mouseY)) {
            return false;
        }
        if (isShiftDown(client)) {
            openContainerHistory(client, view);
        } else if (view.diff() != null && view.diff().hasChanges()) {
            state.highlightsVisible = !state.highlightsVisible;
        }
        return true;
    }

    private boolean isShiftDown(Minecraft client) {
        //? if >=1.21.9 {
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        client.getWindow(), com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        client.getWindow(), com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
        //?} else {
        /*long window = client.getWindow().getWindow();
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        window, com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                        window, com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
        *///?}
    }

    private void openContainerHistory(Minecraft client, ObservationView view) {
        if (client.player != null) {
            client.player.closeContainer();
        }
        ScreenNavigationAdapter.open(
                client,
                new ContainerSnapshotHistoryScreen(
                        null,
                        view,
                        observations.loadHistory(view.identity()),
                        () -> observations.deleteHistory(view.identity())));
    }

    private void drawSlotChanges(
            AbstractContainerScreen<?> screen,
            Object graphics,
            ObservationView view,
            int mouseX,
            int mouseY,
            boolean renderTooltip
    ) {
        Map<Integer, SlotChange> changes = slotChanges(view);
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.chestdiff$getLeftPos();
        int top = accessor.chestdiff$getTopPos();
        for (Map.Entry<Integer, SlotChange> change : changes.entrySet()) {
            if (change.getKey() < 0 || change.getKey() >= screen.getMenu().slots.size()) {
                continue;
            }
            Slot slot = screen.getMenu().getSlot(change.getKey());
            int x = left + slot.x;
            int y = top + slot.y;
            drawPixelBorder(graphics, x - 1, y - 1, 18, 18, change.getValue().color());
            if (renderTooltip && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                GuiRenderAdapter.tooltip(graphics, change.getValue().tooltip(), mouseX, mouseY);
            }
        }
    }

    private SlotChange hoveredSlotChange(
            AbstractContainerScreen<?> screen,
            ObservationView view,
            int mouseX,
            int mouseY
    ) {
        Map<Integer, SlotChange> changes = slotChanges(view);
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.chestdiff$getLeftPos();
        int top = accessor.chestdiff$getTopPos();
        for (Map.Entry<Integer, SlotChange> change : changes.entrySet()) {
            if (change.getKey() < 0 || change.getKey() >= screen.getMenu().slots.size()) {
                continue;
            }
            Slot slot = screen.getMenu().getSlot(change.getKey());
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return change.getValue();
            }
        }
        return null;
    }

    private Map<Integer, SlotChange> slotChanges(ObservationView view) {
        Map<Integer, SlotChange> changes = new HashMap<>();
        for (DiffEntry entry : view.diff().entries()) {
            SlotChange change = toSlotChange(entry, view);
            if (entry.type() == DiffType.REMOVED) {
                entry.beforeSlots().forEach(slot -> changes.put(slot, change));
            } else {
                entry.afterSlots().forEach(slot -> changes.put(slot, change));
            }
        }
        return changes;
    }

    private SlotChange toSlotChange(DiffEntry entry, ObservationView view) {
        List<Component> tooltip = new ArrayList<>();
        int color;
        if (entry.type() == DiffType.ADDED) {
            color = ADDED_COLOR;
            tooltip.add(Component.translatable("chestdiff.tooltip.change.added")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("+" + entry.count() + " " + entry.after().displayName())
                    .withStyle(ChatFormatting.WHITE));
        } else if (entry.type() == DiffType.REMOVED) {
            color = REMOVED_COLOR;
            tooltip.add(Component.translatable("chestdiff.tooltip.change.removed")
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("−" + entry.count() + " " + entry.before().displayName())
                    .withStyle(ChatFormatting.WHITE));
        } else {
            color = MODIFIED_COLOR;
            tooltip.add(Component.translatable("chestdiff.tooltip.change.modified")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable(
                    "chestdiff.tooltip.change.before", entry.before().displayName())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                    "chestdiff.tooltip.change.after", entry.after().displayName())
                    .withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(Component.translatable(
                        "chestdiff.tooltip.change.since", TimeText.relative(view.diff().previousObservedAt()))
                .withStyle(ChatFormatting.DARK_GRAY));
        return new SlotChange(color, List.copyOf(tooltip));
    }

    private IconPosition iconPosition(AbstractContainerScreen<?> screen) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int x = accessor.chestdiff$getLeftPos() - ICON_SIZE - 4;
        int y = accessor.chestdiff$getTopPos() + 5;
        if (x < 2) {
            x = accessor.chestdiff$getLeftPos() + 5;
            y = accessor.chestdiff$getTopPos() - ICON_SIZE - 3;
        }
        return new IconPosition(x, y);
    }

    private void drawNotificationDot(Object graphics, int x, int y) {
        int red = 0xFFFF3B4F;
        GuiRenderAdapter.fill(graphics, x + 1, y, x + 4, y + 1, red);
        GuiRenderAdapter.fill(graphics, x, y + 1, x + 5, y + 4, red);
        GuiRenderAdapter.fill(graphics, x + 1, y + 4, x + 4, y + 5, red);
    }

    private void drawPixelBorder(Object graphics, int x, int y, int width, int height, int color) {
        GuiRenderAdapter.fill(graphics, x, y, x + width, y + 2, color);
        GuiRenderAdapter.fill(graphics, x, y + height - 2, x + width, y + height, color);
        GuiRenderAdapter.fill(graphics, x, y + 2, x + 2, y + height - 2, color);
        GuiRenderAdapter.fill(graphics, x + width - 2, y + 2, x + width, y + height - 2, color);
    }

    private static final class OverlayState {
        private boolean highlightsVisible;
    }

    private record IconPosition(int x, int y) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
        }
    }

    private record SlotChange(int color, List<Component> tooltip) {
    }
}
