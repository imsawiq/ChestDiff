package org.sawiq.chestdiff.identity;

import java.util.Objects;

public record WorldScope(String id, String displayName, boolean singleplayer) {
    public WorldScope {
        Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNullElse(displayName, id);
    }
}
