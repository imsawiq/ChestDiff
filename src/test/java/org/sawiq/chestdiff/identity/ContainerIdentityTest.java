package org.sawiq.chestdiff.identity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ContainerIdentityTest {
    @Test
    void sortedDoubleChestPositionsProduceOneIdentity() {
        WorldScope scope = new WorldScope("server-a", "Server A", false);
        ContainerIdentity fromLeft = block(scope, "overworld:1,64,1|overworld:2,64,1");
        ContainerIdentity fromRight = block(scope, "overworld:1,64,1|overworld:2,64,1");

        assertEquals(fromLeft.stableKey(), fromRight.stableKey());
    }

    @Test
    void sameCoordinatesOnDifferentServersDoNotCollide() {
        ContainerIdentity first = block(new WorldScope("server-a", "A", false), "overworld:1,64,1");
        ContainerIdentity second = block(new WorldScope("server-b", "B", false), "overworld:1,64,1");

        assertNotEquals(first.fileHash(), second.fileHash());
    }

    @Test
    void enderStorageDoesNotDependOnPlacedBlock() {
        WorldScope scope = new WorldScope("server", "Server", false);
        ContainerIdentity first = ender(scope, "player-uuid");
        ContainerIdentity second = ender(scope, "player-uuid");

        assertEquals(first.stableKey(), second.stableKey());
    }

    @Test
    void replacementEpochCreatesNewIdentity() {
        WorldScope scope = new WorldScope("server", "Server", false);
        ContainerIdentity before = new ContainerIdentity(
                scope, IdentityType.BLOCK, ContainerKind.CHEST, "overworld:1,64,1", 0,
                "Chest", "overworld", List.of("1,64,1"), true);
        ContainerIdentity after = new ContainerIdentity(
                scope, IdentityType.BLOCK, ContainerKind.CHEST, "overworld:1,64,1", 1,
                "Chest", "overworld", List.of("1,64,1"), true);

        assertNotEquals(before.stableKey(), after.stableKey());
    }

    @Test
    void entityUuidSeparatesMinecarts() {
        WorldScope scope = new WorldScope("server", "Server", false);
        ContainerIdentity first = entity(scope, "uuid-1");
        ContainerIdentity second = entity(scope, "uuid-2");

        assertNotEquals(first.stableKey(), second.stableKey());
    }

    private static ContainerIdentity block(WorldScope scope, String locator) {
        return new ContainerIdentity(
                scope, IdentityType.BLOCK, ContainerKind.CHEST, locator, 0,
                "Chest", "overworld", List.of(locator), true);
    }

    private static ContainerIdentity ender(WorldScope scope, String player) {
        return new ContainerIdentity(
                scope, IdentityType.ENDER_STORAGE, ContainerKind.ENDER_STORAGE, player, 0,
                "Ender Chest", "", List.of(), true);
    }

    private static ContainerIdentity entity(WorldScope scope, String uuid) {
        return new ContainerIdentity(
                scope, IdentityType.ENTITY, ContainerKind.ENTITY, uuid, 0,
                "Chest Minecart", "overworld", List.of(), true);
    }
}
