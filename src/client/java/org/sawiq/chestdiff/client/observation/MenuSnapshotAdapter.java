package org.sawiq.chestdiff.client.observation;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sawiq.chestdiff.client.compat.StackFingerprintAdapter;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.snapshot.SlotState;

import java.util.ArrayList;
import java.util.List;

public final class MenuSnapshotAdapter {
    private static final int PLAYER_INVENTORY_MENU_SLOTS = 36;
    private final StackFingerprintAdapter fingerprintAdapter = new StackFingerprintAdapter();

    public ContainerSnapshot capture(Minecraft client, AbstractContainerMenu menu, String title) {
        int containerSlotCount = containerSlotCount(menu);
        List<SlotState> slots = new ArrayList<>(containerSlotCount);
        for (int index = 0; index < containerSlotCount; index++) {
            ItemStack stack = menu.getSlot(index).getItem();
            if (!stack.isEmpty()) {
                slots.add(new SlotState(index, fingerprintAdapter.fingerprint(client, stack), stack.getCount()));
            }
        }
        long gameTick = client.level == null ? 0 : client.level.getGameTime();
        return ContainerSnapshot.create(gameTick, title, containerSlotCount, slots);
    }

    public long synchronizationSignature(AbstractContainerMenu menu) {
        int slotCount = containerSlotCount(menu);
        long signature = 0xcbf29ce484222325L;
        for (int index = 0; index < slotCount; index++) {
            ItemStack stack = menu.getSlot(index).getItem();
            signature ^= stack.isEmpty() ? 0 : BuiltInItemSignature.of(stack);
            signature *= 0x100000001b3L;
        }
        return signature;
    }

    public int containerSlotCount(AbstractContainerMenu menu) {
        return Math.max(0, menu.slots.size() - PLAYER_INVENTORY_MENU_SLOTS);
    }

    private static final class BuiltInItemSignature {
        private static long of(ItemStack stack) {
            long itemHash = System.identityHashCode(stack.getItem());
            return (itemHash << 32) ^ (stack.getCount() * 31L) ^ stack.getDamageValue();
        }
    }
}
