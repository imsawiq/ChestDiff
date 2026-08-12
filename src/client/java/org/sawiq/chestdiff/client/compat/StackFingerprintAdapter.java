package org.sawiq.chestdiff.client.compat;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.sawiq.chestdiff.snapshot.ItemFingerprint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class StackFingerprintAdapter {
    public ItemFingerprint fingerprint(Minecraft client, ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String displayName = stack.getHoverName().getString();
        String canonical = serializeComponents(client, stack);
        return new ItemFingerprint(hash(itemId + '\n' + canonical), itemId, displayName, canonical, stack.getDamageValue());
    }

    private String serializeComponents(Minecraft client, ItemStack stack) {
        try {
            ItemStack normalized = stack.copyWithCount(1);
            JsonElement serialized = ItemStack.CODEC
                    .encodeStart(client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), normalized)
                    .result()
                    .orElseThrow();
            return serialized.toString();
        } catch (RuntimeException exception) {
            return "{fallback_item:" + BuiltInRegistries.ITEM.getKey(stack.getItem())
                    + ",damage:" + stack.getDamageValue()
                    + ",name:" + stack.getHoverName().getString() + '}';
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
