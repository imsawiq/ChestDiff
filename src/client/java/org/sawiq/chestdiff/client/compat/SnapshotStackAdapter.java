package org.sawiq.chestdiff.client.compat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.sawiq.chestdiff.snapshot.ItemFingerprint;

public final class SnapshotStackAdapter {
    public ItemStack decode(Minecraft client, ItemFingerprint fingerprint, int count) {
        if (client.level == null) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = ItemStack.CODEC.parse(
                            client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                            JsonParser.parseString(fingerprint.canonicalComponents()))
                    .result()
                    .orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                stack.setCount(Math.min(count, stack.getMaxStackSize()));
            }
            return stack;
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }
}
