package org.sawiq.chestdiff.config;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ChestDiffConfig {
    public static final int SCHEMA_VERSION = 1;

    private int schemaVersion = SCHEMA_VERSION;
    private boolean overlayEnabled = true;
    private int overlayDurationSeconds = 8;
    private boolean animationsEnabled = true;
    private boolean showRearrangements = true;
    private int snapshotsPerContainer = 20;
    private int retentionDays = 30;
    private int diskCapMegabytes = 256;
    private boolean saveVirtualContainers;
    private boolean recordUtilityContainers = true;
    private boolean debugLogging;
    private String coordinateCopyFormat = "{dimension} {x} {y} {z}";
    private String timeDisplayFormat = "relative";
    private Set<String> pinnedContainers = new HashSet<>();

    public int schemaVersion() {
        return schemaVersion;
    }

    public boolean overlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean overlayEnabled) {
        this.overlayEnabled = overlayEnabled;
    }

    public int overlayDurationSeconds() {
        return overlayDurationSeconds;
    }

    public void setOverlayDurationSeconds(int seconds) {
        overlayDurationSeconds = Math.clamp(seconds, 1, 60);
    }

    public boolean animationsEnabled() {
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }

    public boolean showRearrangements() {
        return showRearrangements;
    }

    public void setShowRearrangements(boolean showRearrangements) {
        this.showRearrangements = showRearrangements;
    }

    public int snapshotsPerContainer() {
        return snapshotsPerContainer;
    }

    public void setSnapshotsPerContainer(int snapshots) {
        snapshotsPerContainer = Math.clamp(snapshots, 2, 100);
    }

    public int retentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int days) {
        retentionDays = Math.clamp(days, 1, 3650);
    }

    public int diskCapMegabytes() {
        return diskCapMegabytes;
    }

    public void setDiskCapMegabytes(int megabytes) {
        diskCapMegabytes = Math.clamp(megabytes, 32, 2048);
    }

    public boolean saveVirtualContainers() {
        return saveVirtualContainers;
    }

    public void setSaveVirtualContainers(boolean saveVirtualContainers) {
        this.saveVirtualContainers = saveVirtualContainers;
    }

    public boolean recordUtilityContainers() {
        return recordUtilityContainers;
    }

    public void setRecordUtilityContainers(boolean recordUtilityContainers) {
        this.recordUtilityContainers = recordUtilityContainers;
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public String coordinateCopyFormat() {
        return coordinateCopyFormat;
    }

    public void setCoordinateCopyFormat(String format) {
        coordinateCopyFormat = Objects.requireNonNullElse(format, "{dimension} {x} {y} {z}");
    }

    public String timeDisplayFormat() {
        return timeDisplayFormat;
    }

    public void setTimeDisplayFormat(String format) {
        timeDisplayFormat = Objects.requireNonNullElse(format, "relative");
    }

    public Set<String> pinnedContainers() {
        if (pinnedContainers == null) {
            pinnedContainers = new HashSet<>();
        }
        return Set.copyOf(pinnedContainers);
    }

    public boolean isPinned(String stableKey) {
        return pinnedContainers().contains(stableKey);
    }

    public void setPinned(String stableKey, boolean isPinned) {
        if (pinnedContainers == null) {
            pinnedContainers = new HashSet<>();
        }
        if (isPinned) {
            pinnedContainers.add(stableKey);
        } else {
            pinnedContainers.remove(stableKey);
        }
    }

    public void normalize() {
        schemaVersion = SCHEMA_VERSION;
        setOverlayDurationSeconds(overlayDurationSeconds);
        setSnapshotsPerContainer(snapshotsPerContainer);
        setRetentionDays(retentionDays);
        setDiskCapMegabytes(diskCapMegabytes);
        setCoordinateCopyFormat(coordinateCopyFormat);
        setTimeDisplayFormat(timeDisplayFormat);
        if (pinnedContainers == null) {
            pinnedContainers = new HashSet<>();
        }
    }
}
