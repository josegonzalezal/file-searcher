package com.josegonzalezal.filesearcher.service;

import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.model.SearchCriteria;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Walks filesystem trees safely with cancellation support, permission error resilience,
 * and incremental progress callbacks.
 */
public final class FileSystemScannerService {

    public record ScanProgress(long scannedEntries, int matchedEntries, String currentPath) {
    }

    public record ScanSummary(int matchedCount, long scannedCount, boolean cancelled, boolean limitReached) {
    }

    private final LogManager logManager;

    public FileSystemScannerService(LogManager logManager) {
        this.logManager = logManager;
    }

    public ScanSummary scan(
            SearchCriteria criteria,
            int maxResults,
            boolean enforceLimit,
            BooleanSupplier isCancelled,
            Consumer<SearchResultItem> onMatch,
            Consumer<ScanProgress> onProgress) {

        AtomicLong scannedCounter = new AtomicLong(0L);
        AtomicInteger matchedCounter = new AtomicInteger(0);
        AtomicLong lastProgressNanos = new AtomicLong(System.nanoTime());
        final boolean[] limitReached = {false};
        final boolean[] cancelled = {false};

        List<Path> roots = criteria.getRoots();
        for (int i = 0; i < roots.size(); i++) {
            if (isCancelled.getAsBoolean() || limitReached[0]) {
                cancelled[0] = isCancelled.getAsBoolean();
                break;
            }

            Path root = roots.get(i);
            if (!Files.exists(root)) {
                logManager.warn("Search root does not exist, skipping: " + root);
                continue;
            }

            logManager.info("Scanning root (" + (i + 1) + "/" + roots.size() + "): " + root);

            EnumSet<FileVisitOption> options = criteria.isSkipSymbolicLinks()
                    ? EnumSet.noneOf(FileVisitOption.class)
                    : EnumSet.of(FileVisitOption.FOLLOW_LINKS);

            try {
                Files.walkFileTree(root, options, Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (isCancelled.getAsBoolean()) {
                            cancelled[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        if (limitReached[0]) {
                            return FileVisitResult.TERMINATE;
                        }
                        if (!dir.equals(root) && shouldSkipEntry(dir, attrs, criteria)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        long scanned = scannedCounter.incrementAndGet();
                        publishProgressIfNeeded(scanned, matchedCounter.get(), dir, onProgress, lastProgressNanos, false);
                        evaluateEntry(dir, true, attrs, criteria, maxResults, enforceLimit,
                                matchedCounter, limitReached, onMatch);
                        return limitReached[0] ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isCancelled.getAsBoolean()) {
                            cancelled[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        if (limitReached[0]) {
                            return FileVisitResult.TERMINATE;
                        }
                        long scanned = scannedCounter.incrementAndGet();
                        publishProgressIfNeeded(scanned, matchedCounter.get(), file, onProgress, lastProgressNanos, false);

                        if (!shouldSkipEntry(file, attrs, criteria)) {
                            evaluateEntry(file, false, attrs, criteria, maxResults, enforceLimit,
                                    matchedCounter, limitReached, onMatch);
                        }
                        return limitReached[0] ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        if (isCancelled.getAsBoolean()) {
                            cancelled[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        logManager.warn("Skipped inaccessible path: " + file + " ("
                                + (exc != null ? exc.getClass().getSimpleName() : "I/O error") + ")");
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        if (isCancelled.getAsBoolean()) {
                            cancelled[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        if (exc != null) {
                            logManager.warn("Error after visiting directory: " + dir + " (" + exc.getMessage() + ")");
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                if (!isCancelled.getAsBoolean()) {
                    logManager.error("Failed scanning root: " + root, ex);
                }
            }
        }

        cancelled[0] = cancelled[0] || isCancelled.getAsBoolean();
        publishProgressIfNeeded(
                scannedCounter.get(),
                matchedCounter.get(),
                roots.isEmpty() ? Path.of("") : roots.get(roots.size() - 1),
                onProgress,
                lastProgressNanos,
                true);

        return new ScanSummary(matchedCounter.get(), scannedCounter.get(), cancelled[0], limitReached[0]);
    }

    private boolean shouldSkipEntry(Path entry, BasicFileAttributes attrs, SearchCriteria criteria) {
        if (criteria.isSkipSymbolicLinks() && (attrs.isSymbolicLink() || Files.isSymbolicLink(entry))) {
            return true;
        }
        if (!criteria.isIncludeHidden()) {
            try {
                if (Files.isHidden(entry)) {
                    return true;
                }
            } catch (IOException ex) {
                logManager.warn("Could not determine hidden status for: " + entry);
            }
        }
        return false;
    }

    private void evaluateEntry(
            Path entry,
            boolean isDirectory,
            BasicFileAttributes attrs,
            SearchCriteria criteria,
            int maxResults,
            boolean enforceLimit,
            AtomicInteger matchedCounter,
            boolean[] limitReached,
            Consumer<SearchResultItem> onMatch) {

        if (!criteria.matchesTargetType(isDirectory)) {
            return;
        }

        Path fileNamePath = entry.getFileName();
        String name = (fileNamePath != null) ? fileNamePath.toString() : entry.toString();

        if (!criteria.matchesName(name)) {
            return;
        }

        SearchResultItem item = new SearchResultItem(
                entry,
                isDirectory,
                isDirectory ? 0L : attrs.size(),
                attrs.lastModifiedTime() != null ? attrs.lastModifiedTime().toInstant() : Instant.EPOCH);

        if (!criteria.matchesExtension(item.getExtension(), isDirectory)) {
            return;
        }

        if (!criteria.matchesSize(item.getSizeBytes(), isDirectory)) {
            return;
        }

        if (enforceLimit && matchedCounter.get() >= maxResults) {
            limitReached[0] = true;
            return;
        }

        matchedCounter.incrementAndGet();
        onMatch.accept(item);
    }

    private void publishProgressIfNeeded(
            long scanned,
            int matched,
            Path current,
            Consumer<ScanProgress> onProgress,
            AtomicLong lastProgressNanos,
            boolean force) {
        if (onProgress == null) {
            return;
        }
        long now = System.nanoTime();
        if (force || (now - lastProgressNanos.get()) >= 120_000_000L) {
            lastProgressNanos.set(now);
            String pathStr = current != null ? current.toAbsolutePath().toString() : "";
            onProgress.accept(new ScanProgress(scanned, matched, pathStr));
        }
    }

    public static BasicFileAttributes readAttributesSafe(Path path, boolean skipSymlinks) throws IOException {
        LinkOption[] options = skipSymlinks ? new LinkOption[]{LinkOption.NOFOLLOW_LINKS} : new LinkOption[0];
        return Files.readAttributes(path, BasicFileAttributes.class, options);
    }
}