package org.sawiq.chestdiff.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.sawiq.chestdiff.ChestDiff;

public final class HistoryMigration {
    public JsonObject migrate(JsonObject source) {
        JsonObject migrated = source.deepCopy();
        int version = readVersion(migrated);
        if (version > ChestDiff.DATA_SCHEMA_VERSION) {
            throw new IllegalArgumentException("History was created by a newer ChestDiff version");
        }
        if (version < 1) {
            migrated.addProperty("schemaVersion", 1);
            version = 1;
        }
        if (version == 1) {
            if (!migrated.has("pinned")) {
                migrated.addProperty("pinned", false);
            }
            migrated.addProperty("schemaVersion", 2);
        }
        return migrated;
    }

    private int readVersion(JsonObject object) {
        JsonElement version = object.get("schemaVersion");
        return version == null || !version.isJsonPrimitive() ? 0 : version.getAsInt();
    }
}
