package org.sawiq.chestdiff.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int chestdiff$getLeftPos();

    @Accessor("topPos")
    int chestdiff$getTopPos();

    @Accessor("imageWidth")
    int chestdiff$getImageWidth();

    @Accessor("imageHeight")
    int chestdiff$getImageHeight();
}
