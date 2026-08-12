package org.sawiq.chestdiff.client.identity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public final class InteractionCorrelation {
    private static final long MAX_CORRELATION_TICKS = 12;
    private CorrelatedTarget latest;
    private boolean wasUsePressed;

    public void tick(Minecraft client) {
        boolean usePressed = client.options.keyUse.isDown();
        if (usePressed && !wasUsePressed && client.level != null) {
            HitResult hit = client.hitResult;
            if (hit instanceof BlockHitResult blockHit) {
                latest = CorrelatedTarget.block(blockHit, client.level.getGameTime());
            } else if (hit instanceof EntityHitResult entityHit) {
                latest = CorrelatedTarget.entity(entityHit.getEntity(), client.level.getGameTime());
            }
        }
        wasUsePressed = usePressed;
    }

    public Optional<CorrelatedTarget> consume(Minecraft client) {
        if (latest == null || client.level == null) {
            return Optional.empty();
        }
        CorrelatedTarget result = latest;
        latest = null;
        return client.level.getGameTime() - result.interactionTick() <= MAX_CORRELATION_TICKS
                ? Optional.of(result)
                : Optional.empty();
    }

    public record CorrelatedTarget(BlockHitResult blockHit, Entity entity, long interactionTick) {
        public static CorrelatedTarget block(BlockHitResult hit, long tick) {
            return new CorrelatedTarget(hit, null, tick);
        }

        public static CorrelatedTarget entity(Entity entity, long tick) {
            return new CorrelatedTarget(null, entity, tick);
        }

        public boolean isBlock() {
            return blockHit != null;
        }
    }
}
