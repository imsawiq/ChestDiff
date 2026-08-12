package org.sawiq.chestdiff.config;

import com.google.gson.Gson;
import org.sawiq.chestdiff.storage.JsonCodec;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ConfigRepository implements AutoCloseable {
    private final Path configFile;
    private final Gson gson = JsonCodec.create(true);
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ChestDiff config writer");
        thread.setDaemon(true);
        return thread;
    });

    public ConfigRepository(Path configDirectory) {
        configFile = configDirectory.resolve("chestdiff").resolve("config.json");
    }

    public ChestDiffConfig load() {
        if (!Files.isRegularFile(configFile)) {
            return new ChestDiffConfig();
        }
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            ChestDiffConfig config = gson.fromJson(reader, ChestDiffConfig.class);
            if (config == null) {
                return new ChestDiffConfig();
            }
            config.normalize();
            return config;
        } catch (RuntimeException | IOException exception) {
            preserveCorruptFile();
            return new ChestDiffConfig();
        }
    }

    public CompletableFuture<Void> save(ChestDiffConfig config) {
        return CompletableFuture.runAsync(() -> writeAtomically(config), writer);
    }

    private void writeAtomically(ChestDiffConfig config) {
        Path temporary = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer output = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                gson.toJson(config, output);
            }
            moveAtomically(temporary, configFile);
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // The next successful save replaces this harmless temporary file.
            }
            throw new IllegalStateException("Could not save ChestDiff configuration", exception);
        }
    }

    private void preserveCorruptFile() {
        try {
            Files.move(
                    configFile,
                    configFile.resolveSibling("config.corrupt-" + System.currentTimeMillis() + ".json"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Loading still falls back to defaults even if the corrupt file cannot be renamed.
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException exception) {
            writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
