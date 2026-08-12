package org.sawiq.chestdiff.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.sawiq.chestdiff.config.ChestDiffConfig;
import org.sawiq.chestdiff.identity.ContainerIdentity;
import org.sawiq.chestdiff.identity.IdentityType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class HistoryStorage implements AutoCloseable {
    private static final int MAX_FILES_TO_LOAD = 10_000;

    private final Path dataRoot;
    private final Supplier<ChestDiffConfig> configSupplier;
    private final Gson gson = JsonCodec.create(false);
    private final HistoryMigration migration = new HistoryMigration();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ChestDiff history I/O");
        thread.setDaemon(true);
        return thread;
    });

    public HistoryStorage(Path gameDirectory, Supplier<ChestDiffConfig> configSupplier) {
        this.dataRoot = gameDirectory.resolve("chestdiff-data").toAbsolutePath().normalize();
        this.configSupplier = configSupplier;
    }

    public CompletableFuture<Optional<ContainerHistory>> load(ContainerIdentity identity) {
        return CompletableFuture.supplyAsync(() -> loadBlocking(historyFile(identity)), ioExecutor);
    }

    public CompletableFuture<Void> save(ContainerHistory history) {
        return CompletableFuture.runAsync(() -> {
            writeBlocking(historyFile(history.identity()), history);
            cleanupToDiskCap();
        }, ioExecutor);
    }

    public CompletableFuture<List<ContainerHistory>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            if (!Files.isDirectory(dataRoot)) {
                return List.of();
            }
            List<ContainerHistory> histories = new ArrayList<>();
            try (Stream<Path> files = Files.walk(dataRoot, 4)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json.gz"))
                        .limit(MAX_FILES_TO_LOAD)
                        .forEach(path -> loadBlocking(path)
                                .filter(HistoryStorage::isVisibleHistory)
                                .ifPresent(histories::add));
            } catch (IOException ignored) {
                return List.of();
            }
            histories.sort(Comparator.comparing(ContainerHistory::lastObservedAt).reversed());
            return List.copyOf(histories);
        }, ioExecutor);
    }

    public CompletableFuture<Void> delete(ContainerIdentity identity) {
        return CompletableFuture.runAsync(() -> {
            Path file = historyFile(identity);
            deleteFileQuietly(file);
            deleteFileQuietly(backupFile(file));
        }, ioExecutor);
    }

    public CompletableFuture<Void> deleteScope(String scopeId) {
        return CompletableFuture.runAsync(() -> deleteTree(scopeDirectory(scopeId)), ioExecutor);
    }

    public CompletableFuture<Void> deleteAll() {
        return CompletableFuture.runAsync(() -> deleteTree(dataRoot), ioExecutor);
    }

    private Optional<ContainerHistory> loadBlocking(Path file) {
        Optional<ContainerHistory> primary = readFile(file);
        if (primary.isPresent()) {
            return primary;
        }
        return readFile(backupFile(file));
    }

    private static boolean isVisibleHistory(ContainerHistory history) {
        return history.identity().type() != IdentityType.ENDER_STORAGE;
    }

    private Optional<ContainerHistory> readFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(file));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {
            JsonObject source = JsonParser.parseReader(reader).getAsJsonObject();
            ContainerHistory history = gson.fromJson(migration.migrate(source), ContainerHistory.class);
            return history == null ? Optional.empty() : Optional.of(history);
        } catch (RuntimeException | IOException exception) {
            return Optional.empty();
        }
    }

    private void writeBlocking(Path file, ContainerHistory history) {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(temporary));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8))) {
                gson.toJson(history, writer);
            }
            if (Files.isRegularFile(file)) {
                Files.copy(file, backupFile(file), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, file);
            writeManifest(history.identity());
        } catch (IOException exception) {
            deleteFileQuietly(temporary);
            throw new IllegalStateException("Could not save ChestDiff history", exception);
        }
    }

    private void writeManifest(ContainerIdentity identity) throws IOException {
        Path manifest = scopeDirectory(identity.worldScope().id()).resolve("manifest.json");
        Path temporary = manifest.resolveSibling("manifest.json.tmp");
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", 1);
        object.addProperty("scopeId", identity.worldScope().id());
        object.addProperty("scopeName", identity.worldScope().displayName());
        object.addProperty("updatedAt", Instant.now().toString());
        try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            gson.toJson(object, writer);
        }
        moveAtomically(temporary, manifest);
    }

    private void cleanupToDiskCap() {
        if (!Files.isDirectory(dataRoot)) {
            return;
        }
        long capBytes = configSupplier.get().diskCapMegabytes() * 1024L * 1024L;
        try (Stream<Path> paths = Files.walk(dataRoot, 4)) {
            List<FileRecord> files = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json.gz"))
                    .map(this::fileRecord)
                    .filter(record -> record != null)
                    .sorted(Comparator.comparing(FileRecord::pinned)
                            .thenComparing(FileRecord::lastModified))
                    .toList();
            long total = files.stream().mapToLong(FileRecord::size).sum();
            for (FileRecord file : files) {
                if (total <= capBytes) {
                    break;
                }
                deleteFileQuietly(file.path());
                deleteFileQuietly(backupFile(file.path()));
                total -= file.size();
            }
        } catch (IOException ignored) {
            // Cleanup is best-effort; a future successful save retries it.
        }
    }

    private FileRecord fileRecord(Path path) {
        try {
            Path backup = backupFile(path);
            long backupSize = Files.isRegularFile(backup) ? Files.size(backup) : 0L;
            boolean pinned = loadBlocking(path).map(ContainerHistory::pinned).orElse(false);
            return new FileRecord(
                    path,
                    Files.size(path) + backupSize,
                    Files.getLastModifiedTime(path),
                    pinned);
        } catch (IOException ignored) {
            return null;
        }
    }

    private Path historyFile(ContainerIdentity identity) {
        return scopeDirectory(identity.worldScope().id())
                .resolve("containers")
                .resolve(identity.fileHash() + ".json.gz");
    }

    private Path scopeDirectory(String scopeId) {
        String safeScope = Integer.toHexString(scopeId.hashCode()) + "-" + Integer.toUnsignedString(scopeId.hashCode(), 36);
        return dataRoot.resolve(safeScope).normalize();
    }

    private Path backupFile(Path file) {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    private void deleteTree(Path target) {
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(dataRoot) || normalized.equals(normalized.getRoot())) {
            throw new IllegalArgumentException("Refusing to delete outside the ChestDiff data directory");
        }
        if (!Files.exists(normalized)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(normalized)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deleteFileQuietly);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete ChestDiff data", exception);
        }
    }

    private void deleteFileQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A locked file remains recoverable and can be retried from the UI.
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
        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private record FileRecord(Path path, long size, FileTime lastModified, boolean pinned) {
    }
}
