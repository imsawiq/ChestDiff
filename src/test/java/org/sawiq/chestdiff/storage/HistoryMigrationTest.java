package org.sawiq.chestdiff.storage;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.sawiq.chestdiff.ChestDiff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HistoryMigrationTest {
    @Test
    void versionOneHistoryGetsPinnedFieldAndCurrentSchema() {
        JsonObject old = new JsonObject();
        old.addProperty("schemaVersion", 1);

        JsonObject migrated = new HistoryMigration().migrate(old);

        assertEquals(ChestDiff.DATA_SCHEMA_VERSION, migrated.get("schemaVersion").getAsInt());
        assertFalse(migrated.get("pinned").getAsBoolean());
    }
}
