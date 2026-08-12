package org.sawiq.chestdiff.diff;

import org.junit.jupiter.api.Test;
import org.sawiq.chestdiff.snapshot.ContainerSnapshot;
import org.sawiq.chestdiff.snapshot.ItemFingerprint;
import org.sawiq.chestdiff.snapshot.SlotState;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticDiffEngineTest {
    private static final ItemFingerprint DIAMOND = item("minecraft:diamond", "diamond");
    private static final ItemFingerprint IRON = item("minecraft:iron_ingot", "iron");
    private final SemanticDiffEngine engine = new SemanticDiffEngine();

    @Test
    void identicalSnapshotsHaveNoChanges() {
        ContainerDiff diff = engine.compare(snapshot(slot(0, DIAMOND, 64)), snapshot(slot(0, DIAMOND, 64)));

        assertFalse(diff.hasChanges());
    }

    @Test
    void newItemIsAdded() {
        ContainerDiff diff = engine.compare(snapshot(), snapshot(slot(2, DIAMOND, 17)));

        assertEquals(17, diff.entriesOfType(DiffType.ADDED).getFirst().count());
        assertTrue(diff.entriesOfType(DiffType.REMOVED).isEmpty());
    }

    @Test
    void missingItemIsRemoved() {
        ContainerDiff diff = engine.compare(snapshot(slot(2, DIAMOND, 17)), snapshot());

        assertEquals(17, diff.entriesOfType(DiffType.REMOVED).getFirst().count());
    }

    @Test
    void countIncreaseReportsOnlyDelta() {
        ContainerDiff diff = engine.compare(snapshot(slot(0, IRON, 12)), snapshot(slot(0, IRON, 31)));

        assertEquals(19, diff.entriesOfType(DiffType.ADDED).getFirst().count());
    }

    @Test
    void countDecreaseReportsOnlyDelta() {
        ContainerDiff diff = engine.compare(snapshot(slot(0, IRON, 31)), snapshot(slot(0, IRON, 12)));

        assertEquals(19, diff.entriesOfType(DiffType.REMOVED).getFirst().count());
    }

    @Test
    void pureSlotMoveIsRearrangementNotAddRemove() {
        ContainerDiff diff = engine.compare(snapshot(slot(2, DIAMOND, 64)), snapshot(slot(8, DIAMOND, 64)));

        assertTrue(diff.entries().isEmpty());
        assertEquals(2, diff.rearrangedSlots());
    }

    @Test
    void consolidationIsRearrangementNotQuantityChange() {
        ContainerDiff diff = engine.compare(
                snapshot(slot(0, IRON, 20), slot(1, IRON, 30)),
                snapshot(slot(3, IRON, 50)));

        assertTrue(diff.entries().isEmpty());
        assertEquals(3, diff.rearrangedSlots());
    }

    @Test
    void splitIsRearrangementNotQuantityChange() {
        ContainerDiff diff = engine.compare(
                snapshot(slot(0, IRON, 50)),
                snapshot(slot(2, IRON, 20), slot(3, IRON, 30)));

        assertTrue(diff.entries().isEmpty());
        assertEquals(3, diff.rearrangedSlots());
    }

    @Test
    void multipleIdenticalStacksCanMoveWithoutSpam() {
        ContainerDiff diff = engine.compare(
                snapshot(slot(0, IRON, 64), slot(1, IRON, 64)),
                snapshot(slot(5, IRON, 64), slot(7, IRON, 64)));

        assertTrue(diff.entries().isEmpty());
        assertEquals(4, diff.rearrangedSlots());
    }

    @Test
    void durabilityChangeInSameSlotPairsAsModification() {
        ItemFingerprint pristine = new ItemFingerprint(
                "pickaxe-a", "minecraft:diamond_pickaxe", "Diamond Pickaxe", "{damage:10}", 10);
        ItemFingerprint worn = new ItemFingerprint(
                "pickaxe-b", "minecraft:diamond_pickaxe", "Diamond Pickaxe", "{damage:90}", 90);

        ContainerDiff diff = engine.compare(snapshot(slot(4, pristine, 1)), snapshot(slot(4, worn, 1)));

        assertEquals(1, diff.entriesOfType(DiffType.MODIFIED).size());
        assertTrue(diff.entriesOfType(DiffType.ADDED).isEmpty());
        assertTrue(diff.entriesOfType(DiffType.REMOVED).isEmpty());
    }

    @Test
    void enchantedAndPlainStacksRemainDifferentFingerprints() {
        ItemFingerprint plain = item("minecraft:diamond_sword", "plain");
        ItemFingerprint enchanted = item("minecraft:diamond_sword", "enchanted");

        assertFalse(plain.equals(enchanted));
    }

    @Test
    void shulkerContentsParticipateInFingerprint() {
        ItemFingerprint empty = item("minecraft:shulker_box", "contents-empty");
        ItemFingerprint full = item("minecraft:shulker_box", "contents-diamonds");

        assertFalse(empty.equals(full));
    }

    private static ContainerSnapshot snapshot(SlotState... slots) {
        return new ContainerSnapshot(
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                100,
                "Chest",
                27,
                List.of(slots));
    }

    private static SlotState slot(int index, ItemFingerprint item, int count) {
        return new SlotState(index, item, count);
    }

    private static ItemFingerprint item(String id, String canonical) {
        return new ItemFingerprint(canonical, id, id, canonical, 0);
    }
}
