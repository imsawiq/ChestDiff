package org.sawiq.chestdiff.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sawiq.chestdiff.client.observation.ObservationView;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.diff.ContainerDiff;
import org.sawiq.chestdiff.diff.SemanticDiffEngine;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.storage.ContainerHistory;
import org.sawiq.chestdiff.storage.HistoryStorage;

import java.util.List;

public final class HistoryScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private final Screen parent;
    private final HistoryStorage storage;
    private final Runnable openSettings;
    private final SemanticDiffEngine diffEngine = new SemanticDiffEngine();
    private List<ContainerHistory> histories = List.of();
    private boolean loading = true;
    private int page;

    public HistoryScreen(Screen parent, HistoryStorage storage, Runnable openSettings) {
        super(Component.translatable("chestdiff.screen.history"));
        this.parent = parent;
        this.storage = storage;
        this.openSettings = openSettings;
    }

    @Override
    protected void init() {
        int rowsPerPage = rowsPerPage();
        int start = page * rowsPerPage;
        int end = Math.min(histories.size(), start + rowsPerPage);
        for (int index = start; index < end; index++) {
            ContainerHistory history = histories.get(index);
            int y = 42 + (index - start) * ROW_HEIGHT;
            addRenderableWidget(Button.builder(rowTitle(history), button -> openHistory(history))
                    .bounds(width / 2 - 145, y, 290, 28)
                    .build());
        }

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildWidgets();
                })
                .bounds(width / 2 - 145, height - 27, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    int maximumPage = Math.max(0, (histories.size() - 1) / rowsPerPage());
                    page = Math.min(maximumPage, page + 1);
                    rebuildWidgets();
                })
                .bounds(width / 2 - 117, height - 27, 24, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.settings"), button -> openSettings.run())
                .bounds(width / 2 - 50, height - 27, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.done"), button -> onClose())
                .bounds(width / 2 + 50, height - 27, 95, 20).build());

        if (loading) {
            storage.loadAll().thenAccept(loaded -> minecraft.execute(() -> {
                histories = loaded;
                loading = false;
                rebuildWidgets();
            }));
        }
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderContents(graphics);
    }
    //?} else {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderContents(graphics);
    }
    *///?}

    private void renderContents(Object graphics) {
        GuiRenderAdapter.centered(graphics, title, width / 2, 16, 0xFFFFFFFF);
        if (loading) {
            GuiRenderAdapter.centered(graphics, Component.literal("Loading…"), width / 2, 30, 0xFF9EA6B3);
        } else if (histories.isEmpty()) {
            GuiRenderAdapter.centered(
                    graphics, Component.translatable("chestdiff.empty.history"), width / 2, height / 2, 0xFF9EA6B3);
        }
    }

    @Override
    public void onClose() {
        ScreenNavigationAdapter.open(minecraft, parent);
    }

    private void openHistory(ContainerHistory history) {
        if (history.snapshots().isEmpty()) return;
        ContainerSnapshot current = history.snapshots().getLast();
        if (history.snapshots().size() == 1) {
            ScreenNavigationAdapter.open(minecraft, new DiffScreen(
                    this,
                    ObservationView.first(history.identity(), current),
                    () -> storage.delete(history.identity())));
            return;
        }
        ContainerSnapshot previous = history.snapshots().get(history.snapshots().size() - 2);
        ContainerDiff diff = diffEngine.compare(previous, current);
        ScreenNavigationAdapter.open(minecraft, new DiffScreen(
                this,
                ObservationView.compared(history.identity(), previous, current, diff),
                () -> storage.delete(history.identity())));
    }

    private Component rowTitle(ContainerHistory history) {
        return Component.literal((history.pinned() ? "★ " : "")
                + history.identity().displayName()
                + locationSuffix(history)
                + "  •  " + TimeText.relative(history.lastObservedAt())
                + "  •  " + history.snapshots().size());
    }

    private String locationSuffix(ContainerHistory history) {
        List<String> positions = history.identity().positions();
        if (positions.isEmpty()) {
            return "";
        }
        String dimension = shortDimension(history.identity().dimension());
        String coordinates = positions.stream()
                .map(position -> position.replace(',', ' '))
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
        return "  •  " + dimension + " " + coordinates;
    }

    private String shortDimension(String dimension) {
        int separator = dimension.indexOf(':');
        return separator >= 0 ? dimension.substring(separator + 1) : dimension;
    }

    private int rowsPerPage() {
        return Math.max(1, (height - 84) / ROW_HEIGHT);
    }
}
