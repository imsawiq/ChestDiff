package org.sawiq.chestdiff.client.ui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sawiq.chestdiff.client.observation.ObservationView;
import org.sawiq.chestdiff.client.compat.GuiRenderAdapter;
import org.sawiq.chestdiff.client.compat.ScreenNavigationAdapter;
import org.sawiq.chestdiff.diff.DiffEntry;
import org.sawiq.chestdiff.diff.DiffType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DiffScreen extends Screen {
    private static final int ROW_HEIGHT = 13;
    private final Screen parent;
    private final ObservationView view;
    private final Runnable deleteHistory;
    private EditBox search;
    private int page;

    public DiffScreen(Screen parent, ObservationView view) {
        this(parent, view, null);
    }

    public DiffScreen(Screen parent, ObservationView view, Runnable deleteHistory) {
        super(Component.translatable("chestdiff.screen.diff"));
        this.parent = parent;
        this.view = view;
        this.deleteHistory = deleteHistory;
    }

    @Override
    protected void init() {
        search = new EditBox(font, width / 2 - 110, 38, 220, 18, Component.empty());
        search.setHint(Component.literal("Search…"));
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1))
                .bounds(width / 2 - 110, height - 27, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> page++)
                .bounds(width / 2 - 82, height - 27, 24, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.done"), button -> onClose())
                .bounds(width / 2 + 10, height - 27, 100, 20).build());
        if (!view.identity().positions().isEmpty()) {
            addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.copy_coordinates"), button ->
                            minecraft.keyboardHandler.setClipboard(String.join(" | ", view.identity().positions())))
                    .bounds(width / 2 - 110, height - 51, 130, 20).build());
        }
        if (deleteHistory != null) {
            addRenderableWidget(Button.builder(Component.translatable("chestdiff.button.delete"), button ->
                            ScreenNavigationAdapter.open(minecraft, new ConfirmationScreen(
                                    this,
                                    Component.translatable("chestdiff.button.delete"),
                                    deleteHistory)))
                    .bounds(width / 2 + 25, height - 51, 85, 20).build());
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
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderContents(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    private void renderContents(Object graphics) {
        GuiRenderAdapter.centered(graphics, title, width / 2, 14, 0xFFFFFFFF);
        GuiRenderAdapter.centered(
                graphics,
                Component.translatable("chestdiff.observed", observedText()),
                width / 2,
                26,
                0xFF9EA6B3);

        List<Row> rows = visibleRows();
        int rowsPerPage = Math.max(1, (height - 100) / ROW_HEIGHT);
        int maximumPage = Math.max(0, (rows.size() - 1) / rowsPerPage);
        page = Math.min(page, maximumPage);
        int from = Math.min(rows.size(), page * rowsPerPage);
        int to = Math.min(rows.size(), from + rowsPerPage);
        int y = 64;
        if (rows.isEmpty()) {
            GuiRenderAdapter.centered(graphics, Component.translatable("chestdiff.empty.diff"), width / 2, y, 0xFF9EA6B3);
        } else {
            for (Row row : rows.subList(from, to)) {
                GuiRenderAdapter.text(graphics, row.text(), width / 2 - 140, y, row.color(), false);
                y += ROW_HEIGHT;
            }
        }
        GuiRenderAdapter.text(graphics, (page + 1) + "/" + (maximumPage + 1), width / 2 - 53, height - 21, 0xFF9EA6B3, false);
    }

    @Override
    public void onClose() {
        ScreenNavigationAdapter.open(minecraft, parent);
    }

    private List<Row> visibleRows() {
        List<Row> rows = new ArrayList<>();
        if (view.diff() == null) {
            return rows;
        }
        appendSection(rows, DiffType.ADDED, "chestdiff.section.added", 0xFF55E080);
        appendSection(rows, DiffType.REMOVED, "chestdiff.section.removed", 0xFFFF6476);
        appendSection(rows, DiffType.MODIFIED, "chestdiff.section.modified", 0xFF65A8FF);
        if (view.diff().rearrangedSlots() > 0) {
            rows.add(new Row(Component.translatable("chestdiff.section.rearranged"), 0xFFB88CFF));
            rows.add(new Row(Component.translatable(
                    "chestdiff.entry.rearranged", view.diff().rearrangedSlots()), 0xFFD8C7FF));
        }
        String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
        return query.isEmpty()
                ? rows
                : rows.stream().filter(row -> row.text().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private void appendSection(List<Row> rows, DiffType type, String headingKey, int color) {
        List<DiffEntry> entries = view.diff().entriesOfType(type);
        if (entries.isEmpty()) return;
        rows.add(new Row(Component.translatable(headingKey), color));
        for (DiffEntry entry : entries) {
            Component line = switch (type) {
                case ADDED -> Component.translatable(
                        "chestdiff.entry.added", entry.count(), entry.visibleFingerprint().displayName());
                case REMOVED -> Component.translatable(
                        "chestdiff.entry.removed", entry.count(), entry.visibleFingerprint().displayName());
                case MODIFIED -> Component.translatable(
                        "chestdiff.entry.modified", entry.before().displayName(), entry.after().displayName());
                case REARRANGED -> Component.empty();
            };
            rows.add(new Row(line, 0xFFE7EAF0));
        }
    }

    private String observedText() {
        if (view.diff() == null) return "first observation";
        return TimeText.relative(view.diff().previousObservedAt());
    }

    private record Row(Component text, int color) {
    }
}
