package org.sawiq.chestdiff.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class GuiRenderAdapter {
    private GuiRenderAdapter() {
    }

    public static void fill(Object graphics, int x1, int y1, int x2, int y2, int color) {
        //? if >=26.1 {
        ((net.minecraft.client.gui.GuiGraphicsExtractor) graphics).fill(x1, y1, x2, y2, color);
        //?} else {
        /*((net.minecraft.client.gui.GuiGraphics) graphics).fill(x1, y1, x2, y2, color);
        *///?}
    }

    public static void text(Object graphics, Component text, int x, int y, int color, boolean shadow) {
        // Minecraft 1.21.6+ interprets GUI text colors as ARGB. Keep legacy
        // RGB call sites visible while preserving explicitly supplied alpha.
        //? if >=1.21.6
        color = opaqueIfRgb(color);
        //? if >=26.1 {
        ((net.minecraft.client.gui.GuiGraphicsExtractor) graphics)
                .text(Minecraft.getInstance().font, text, x, y, color, shadow);
        //?} else {
        /*((net.minecraft.client.gui.GuiGraphics) graphics)
                .drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
        *///?}
    }

    public static void text(Object graphics, String text, int x, int y, int color, boolean shadow) {
        text(graphics, Component.literal(text), x, y, color, shadow);
    }

    public static void centered(Object graphics, Component text, int centerX, int y, int color) {
        int x = centerX - Minecraft.getInstance().font.width(text) / 2;
        text(graphics, text, x, y, color, false);
    }

    public static void tooltip(Object graphics, List<Component> lines, int mouseX, int mouseY) {
        List<FormattedCharSequence> visualLines = lines.stream()
                .map(Component::getVisualOrderText)
                .toList();
        //? if >=26.1 {
        ((net.minecraft.client.gui.GuiGraphicsExtractor) graphics)
                .setTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        visualLines,
                        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
                        mouseX,
                        mouseY,
                        true);
        //?} elif >=1.21.6 {
        /*((net.minecraft.client.gui.GuiGraphics) graphics)
                .setTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        visualLines,
                        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
                        mouseX,
                        mouseY,
                        true);
        *///?} else {
        /*((net.minecraft.client.gui.GuiGraphics) graphics)
                .renderTooltip(Minecraft.getInstance().font, lines, java.util.Optional.empty(), mouseX, mouseY);
        *///?}
    }

    public static void renderPendingTooltip(Object graphics) {
        //? if >=1.21.6 && <1.21.9 {
        /*((net.minecraft.client.gui.GuiGraphics) graphics).renderDeferredTooltip();
        *///?}
    }

    private static int opaqueIfRgb(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    public static void item(Object graphics, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        net.minecraft.client.gui.GuiGraphicsExtractor extractor =
                (net.minecraft.client.gui.GuiGraphicsExtractor) graphics;
        extractor.item(stack, x, y);
        extractor.itemDecorations(Minecraft.getInstance().font, stack, x, y);
        //?} else {
        /*net.minecraft.client.gui.GuiGraphics guiGraphics = (net.minecraft.client.gui.GuiGraphics) graphics;
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
        *///?}
    }
}
