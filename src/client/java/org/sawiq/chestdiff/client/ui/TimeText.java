package org.sawiq.chestdiff.client.ui;

import java.time.Duration;
import java.time.Instant;

public final class TimeText {
    private TimeText() {
    }

    public static String relative(Instant time) {
        Duration age = Duration.between(time, Instant.now());
        if (age.isNegative()) return "now";
        long minutes = age.toMinutes();
        if (minutes < 1) return "now";
        if (minutes < 60) return minutes + "m ago";
        long hours = age.toHours();
        if (hours < 24) return hours + "h ago";
        return age.toDays() + "d ago";
    }
}
