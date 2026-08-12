package org.sawiq.chestdiff.client.identity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.identity.ContainerKind;
import org.sawiq.chestdiff.identity.IdentityType;
import org.sawiq.chestdiff.identity.WorldScope;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ContainerIdentityResolver {
    private final Map<String, ObservedBlock> observedBlocks = new HashMap<>();

    public ContainerIdentity resolve(
            Minecraft client,
            Optional<InteractionCorrelation.CorrelatedTarget> target,
            String menuType,
            String title,
            ChestDiffConfig config
    ) {
        WorldScope scope = resolveScope(client);
        if (target.isPresent() && target.get().isBlock() && client.level != null) {
            return resolveBlock(client, scope, target.get().blockHit().getBlockPos(), title);
        }
        if (target.isPresent() && target.get().entity() != null && client.level != null) {
            return resolveEntity(client, scope, target.get().entity(), title);
        }
        String virtualLocator = menuType + '|' + title + '|' + UUID.randomUUID();
        return new ContainerIdentity(
                scope,
                IdentityType.VIRTUAL,
                ContainerKind.VIRTUAL,
                virtualLocator,
                0,
                title,
                dimensionId(client),
                List.of(),
                config.saveVirtualContainers());
    }

    public boolean isEnderStorage(
            Minecraft client,
            Optional<InteractionCorrelation.CorrelatedTarget> target,
            Component screenTitle
    ) {
        if (target.filter(InteractionCorrelation.CorrelatedTarget::isBlock)
                .map(correlatedTarget -> correlatedTarget.blockHit().getBlockPos())
                .filter(position -> client.level != null && client.level.hasChunkAt(position))
                .map(position -> client.level.getBlockState(position).getBlock())
                .filter(EnderChestBlock.class::isInstance)
                .isPresent()) {
            return true;
        }
        return screenTitle.getString().equals(Component.translatable("container.enderchest").getString());
    }

    public void tickKnownBlocks(Minecraft client) {
        if (client.level == null || client.level.getGameTime() % 10 != 0) {
            return;
        }
        for (ObservedBlock observed : observedBlocks.values()) {
            if (!client.level.hasChunkAt(observed.position())) {
                continue;
            }
            Block current = client.level.getBlockState(observed.position()).getBlock();
            if (current != observed.expectedBlock()) {
                observed.markMissing();
            } else if (observed.wasMissing()) {
                observed.incrementEpoch();
            }
        }
    }

    private ContainerIdentity resolveBlock(
            Minecraft client,
            WorldScope scope,
            BlockPos clickedPosition,
            String title
    ) {
        BlockState state = client.level.getBlockState(clickedPosition);
        Block block = state.getBlock();
        ContainerKind kind = classify(block);
        List<BlockPos> positions = canonicalPositions(client, clickedPosition, state);
        String dimension = dimensionId(client);
        List<String> encodedPositions = positions.stream().map(ContainerIdentityResolver::encodePosition).toList();
        String baseLocator = dimension + ':' + String.join("|", encodedPositions);
        ObservedBlock observed = observedBlocks.computeIfAbsent(
                scope.id() + '|' + baseLocator,
                ignored -> new ObservedBlock(clickedPosition.immutable(), block));
        if (observed.expectedBlock() != block) {
            observed.replaceExpectedBlock(block);
        }
        return new ContainerIdentity(
                scope,
                IdentityType.BLOCK,
                kind,
                baseLocator,
                observed.epoch(),
                title,
                dimension,
                encodedPositions,
                kind != ContainerKind.UNKNOWN);
    }

    private ContainerIdentity resolveEntity(Minecraft client, WorldScope scope, Entity entity, String title) {
        return new ContainerIdentity(
                scope,
                IdentityType.ENTITY,
                ContainerKind.ENTITY,
                entity.getUUID().toString(),
                0,
                title,
                dimensionId(client),
                List.of(),
                true);
    }

    private List<BlockPos> canonicalPositions(Minecraft client, BlockPos position, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return List.of(position.immutable());
        }
        List<BlockPos> positions = new ArrayList<>();
        positions.add(position.immutable());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = position.relative(direction);
            BlockState neighborState = client.level.getBlockState(neighbor);
            if (neighborState.getBlock() == state.getBlock()
                    && neighborState.hasProperty(ChestBlock.TYPE)
                    && neighborState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                positions.add(neighbor.immutable());
                break;
            }
        }
        positions.sort(Comparator.comparingLong(BlockPos::asLong));
        return List.copyOf(positions);
    }

    private ContainerKind classify(Block block) {
        if (block instanceof TrappedChestBlock) return ContainerKind.TRAPPED_CHEST;
        if (block instanceof ChestBlock) return ContainerKind.CHEST;
        if (block instanceof BarrelBlock) return ContainerKind.BARREL;
        if (block instanceof ShulkerBoxBlock) return ContainerKind.SHULKER_BOX;
        if (block instanceof HopperBlock) return ContainerKind.HOPPER;
        if (block instanceof DropperBlock) return ContainerKind.DROPPER;
        if (block instanceof DispenserBlock) return ContainerKind.DISPENSER;
        if (block instanceof BlastFurnaceBlock) return ContainerKind.BLAST_FURNACE;
        if (block instanceof SmokerBlock) return ContainerKind.SMOKER;
        if (block instanceof FurnaceBlock) return ContainerKind.FURNACE;
        return ContainerKind.UNKNOWN;
    }

    public WorldScope resolveScope(Minecraft client) {
        String player = client.getUser().getProfileId().toString();
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            String levelName = client.getSingleplayerServer().getWorldData().getLevelName();
            return new WorldScope(hash("singleplayer|" + levelName + '|' + player), levelName, true);
        }
        ServerData server = client.getCurrentServer();
        String address = server == null ? "unknown-server" : server.ip;
        String name = server == null ? "Multiplayer" : server.name;
        return new WorldScope(hash("multiplayer|" + address + '|' + player), name, false);
    }

    private String dimensionId(Minecraft client) {
        if (client.level == null) return "";
        //? if >=1.21.11
        return client.level.dimension().identifier().toString();
        //? if <1.21.11
        /*return client.level.dimension().location().toString();*/
    }

    private static String encodePosition(BlockPos position) {
        return position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class ObservedBlock {
        private final BlockPos position;
        private Block expectedBlock;
        private int epoch;
        private boolean wasMissing;

        private ObservedBlock(BlockPos position, Block expectedBlock) {
            this.position = position;
            this.expectedBlock = expectedBlock;
        }

        BlockPos position() { return position; }
        Block expectedBlock() { return expectedBlock; }
        int epoch() { return epoch; }
        boolean wasMissing() { return wasMissing; }
        void markMissing() { wasMissing = true; }
        void incrementEpoch() { epoch++; wasMissing = false; }
        void replaceExpectedBlock(Block block) { expectedBlock = block; epoch++; wasMissing = false; }
    }
}
