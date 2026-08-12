package org.sawiq.chestdiff.client.observation;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

final class SupportedContainerMenus {
    private SupportedContainerMenus() {
    }

    static boolean shouldObserve(AbstractContainerMenu menu, boolean recordUtilityContainers) {
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
            return true;
        }
        return recordUtilityContainers && isUtilityContainer(menu);
    }

    private static boolean isUtilityContainer(AbstractContainerMenu menu) {
        return menu instanceof HopperMenu
                || menu instanceof DispenserMenu
                || menu instanceof AbstractFurnaceMenu;
    }
}
