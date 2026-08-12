package org.sawiq.chestdiff.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenNavigationAdapter {
    private ScreenNavigationAdapter() {
    }

    public static Screen current(Minecraft client) {
        //? if >=26.2
        return client.gui.screen();
        //? if <26.2
        /*return client.screen;*/
    }

    public static void open(Minecraft client, Screen screen) {
        //? if >=26.2
        client.gui.setScreen(screen);
        //? if <26.2
        /*client.setScreen(screen);*/
    }
}
