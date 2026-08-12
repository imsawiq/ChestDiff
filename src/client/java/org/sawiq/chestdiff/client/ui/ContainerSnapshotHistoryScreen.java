package org.sawiq.chestdiff.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.client.compat.SnapshotStackAdapter;
import org.sawiq.chestdiff.client.observation.ObservationView;
import org.sawiq.chestdiff.diff.ContainerDiff;
import org.sawiq.chestdiff.diff.DiffType;
import org.sawiq.chestdiff.diff.SemanticDiffEngine;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.snapshot.SlotState;
import org.sawiq.chestdiff.storage.ContainerHistory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ContainerSnapshotHistoryScreen extends Screen {
    private static final int SLOT_SIZE = 18;
    private static final int MAX_COLUMNS = 9;

    private final Screen parent;
    private final ObservationView fallbackView;
    private final CompletableFuture<java.util.Optional<ContainerHistory>> historyFuture;
    private final Runnable deleteHistory;
    private final SemanticDiffEngine diffEngine = new SemanticDiffEngine();
    private final SnapshotStackAdapter stackAdapter = new SnapshotStackAdapter();
    private List<ContainerSnapshot> snapshots = List.of();
    private int selectedIndex;
    private boolean loading = true;

    public ContainerSnapshotHistoryScreen(
            Screen parent,
            ObservationView fallbackView,
            CompletableFuture<java.util.Optional<ContainerHistory>> historyFuture,
            Runnable deleteHistory
    ) {
        super(Component.translatable(
                "chestdiff.screen.container_history", fallbackView.identity().displayName()));
        this.parent = parent;
        this.fallbackView = fallbackView;
        this.historyFuture = historyFuture;
        this.deleteHistory = deleteHistory;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("◀"), button -> select(selectedIndex - 1))
                .bounds(width / 2 - 112, height - 28, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), button -> select(selectedIndex + 1))
                .bounds(width / 2 - 78, height - 28, 30, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.delete"), button ->
                        ScreenNavigationAdapter.open(minecraft, new ConfirmationScreen(
                                this,
                                Component.translatable("chestdiff.button.delete"),
                                () -> {
                                    deleteHistory.run();
                                    snapshots = List.of();
                                    selectedIndex = 0;
                                })))
                .bounds(width / 2 - 34, height - 28, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.done"), button -> onClose())
                .bounds(width / 2 + 66, height - 28, 70, 20).build());

        if (loading) {
            historyFuture.thenAccept(history -> minecraft.execute(() -> {
                List<ContainerSnapshot> loaded = new ArrayList<>(
                        history.map(ContainerHistory::snapshots).orElse(List.of()));
                ContainerSnapshot current = fallbackView.current();
                if (current != null && loaded.stream().noneMatch(snapshot ->
                        snapshot.snapshotId().equals(current.snapshotId()))
                        && (loaded.isEmpty() || !loaded.getLast().hasSameContents(current))) {
                    loaded.add(current);
                }
                snapshots = List.copyOf(loaded);
                selectedIndex = Math.max(0, snapshots.size() - 1);
                loading = false;
            }));
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderContents(graphics, mouseX, mouseY);
    }
    //?} else {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderContents(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    private void renderContents(Object graphics, int mouseX, int mouseY) {
        GuiRenderAdapter.centered(graphics, title, width / 2, 15, 0xFFF0F0F0);
        if (loading) {
            GuiRenderAdapter.centered(
                    graphics, Component.translatable("chestdiff.snapshot.loading"), width / 2, 38, 0xFFAAAAAA);
            return;
        }
        if (snapshots.isEmpty()) {
            GuiRenderAdapter.centered(
                    graphics, Component.translatable("chestdiff.empty.history"), width / 2, 38, 0xFFAAAAAA);
            return;
        }

        ContainerSnapshot snapshot = snapshots.get(selectedIndex);
        GuiRenderAdapter.centered(
                graphics,
                Component.translatable("chestdiff.snapshot.position", selectedIndex + 1, snapshots.size()),
                width / 2,
                30,
                0xFFE0E0E0);
        GuiRenderAdapter.centered(
                graphics,
                Component.translatable("chestdiff.snapshot.captured", TimeText.relative(snapshot.capturedAt())),
                width / 2,
                42,
                0xFF969696);

        ContainerDiff diff = selectedIndex > 0
                ? diffEngine.compare(snapshots.get(selectedIndex - 1), snapshot)
                : null;
        Component comparison = diff == null
                ? Component.translatable("chestdiff.snapshot.first")
                : Component.translatable(
                        "chestdiff.snapshot.compare",
                        diff.entriesOfType(DiffType.ADDED).size(),
                        diff.entriesOfType(DiffType.REMOVED).size(),
                        diff.entriesOfType(DiffType.MODIFIED).size());
        GuiRenderAdapter.centered(graphics, comparison, width / 2, 55, 0xFFC0C0C0);

        int columns = Math.max(1, Math.min(MAX_COLUMNS, snapshot.slotCount()));
        int rows = Math.max(1, (snapshot.slotCount() + columns - 1) / columns);
        int gridWidth = columns * SLOT_SIZE;
        int gridHeight = rows * SLOT_SIZE;
        int left = width / 2 - gridWidth / 2;
        int top = Math.max(72, (height - gridHeight) / 2);
        Map<Integer, SlotState> slots = snapshot.slotsByIndex();
        for (int index = 0; index < snapshot.slotCount(); index++) {
            int x = left + index % columns * SLOT_SIZE;
            int y = top + index / columns * SLOT_SIZE;
            drawSlot(graphics, x, y);
            SlotState slot = slots.get(index);
            if (slot != null) {
                ItemStack stack = stackAdapter.decode(minecraft, slot.fingerprint(), slot.count());
                if (stack.isEmpty()) {
                    GuiRenderAdapter.text(graphics, Component.literal("?"), x + 5, y + 5, 0xFFE8E8E8, false);
                } else {
                    GuiRenderAdapter.item(graphics, stack, x + 1, y + 1);
                }
                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    GuiRenderAdapter.tooltip(graphics, List.of(
                            Component.literal(slot.fingerprint().displayName()).withStyle(ChatFormatting.WHITE),
                            Component.translatable("chestdiff.snapshot.stack_count", slot.count())
                                    .withStyle(ChatFormatting.GRAY),
                            Component.literal(slot.fingerprint().itemId()).withStyle(ChatFormatting.DARK_GRAY)
                    ), mouseX, mouseY);
                }
            }
        }
    }

    private void select(int index) {
        if (!snapshots.isEmpty()) {
            selectedIndex = Math.clamp(index, 0, snapshots.size() - 1);
        }
    }

    private void drawSlot(Object graphics, int x, int y) {
        GuiRenderAdapter.fill(graphics, x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF373737);
        GuiRenderAdapter.fill(graphics, x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
        GuiRenderAdapter.fill(graphics, x + 2, y + 2, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0xFF555555);
    }

    @Override
    public void onClose() {
        ScreenNavigationAdapter.open(minecraft, parent);
    }
}
