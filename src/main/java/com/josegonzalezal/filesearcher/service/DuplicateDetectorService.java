package com.josegonzalezal.filesearcher.service;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.model.DuplicateGroup;
import com.josegonzalezal.filesearcher.model.HashAlgorithm;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Multi-stage duplicate file detector supporting size-only, partial head/tail hashing,
 * and streaming MD5 / SHA-256 digests.
 */
public final class DuplicateDetectorService {

    public record DuplicateProgress(String phase, long processedFiles, long totalCandidates, String currentFile) {
    }

    private final LogManager logManager;

    public DuplicateDetectorService(AppConfig config, LogManager logManager) {
        this.logManager = logManager;
    }

    public List<DuplicateGroup> findDuplicates(
            List<Path> roots,
            HashAlgorithm algorithm,
            boolean skipSymbolicLinks,
            boolean includeHidden,
            BooleanSupplier isCancelled,
            Consumer<DuplicateProgress> onProgress) {

        BooleanSupplier cancelCheck = isCancelled != null ? isCancelled : () -> false;

        List<SearchResultItem> regularFiles = collectFiles(
                roots, skipSymbolicLinks, includeHidden, cancelCheck, onProgress);

        if (cancelCheck.getAsBoolean() || regularFiles.isEmpty()) {
            return List.of();
        }

        // Stage 1: Group by exact file size
        Map<Long, List<SearchResultItem>> bySize = new LinkedHashMap<>();
        for (SearchResultItem file : regularFiles) {
            if (cancelCheck.getAsBoolean()) {
                return List.of();
            }
            bySize.computeIfAbsent(file.getSizeBytes(), k -> new ArrayList<>()).add(file);
        }

        List<SearchResultItem> sizeCandidates = new ArrayList<>();
        for (Map.Entry<Long, List<SearchResultItem>> entry : bySize.entrySet()) {
            if (entry.getValue().size() > 1) {
                sizeCandidates.addAll(entry.getValue());
            }
        }

        if (sizeCandidates.isEmpty()) {
            logManager.info("Duplicate scan finished: no files share the same size.");
            return List.of();
        }

        if (algorithm == HashAlgorithm.SIZE_ONLY) {
            return buildGroups(bySize, "SIZE", cancelCheck);
        }

        // Stage 2: Partial head + tail hash for files larger than chunk size
        Map<String, List<SearchResultItem>> byPartialKey = new LinkedHashMap<>();
        long total = sizeCandidates.size();
        long processed = 0L;

        for (SearchResultItem item : sizeCandidates) {
            if (cancelCheck.getAsBoolean()) {
                return List.of();
            }
            processed++;
            publish(onProgress, "Partial hash (head/tail)", processed, total, item.getFullPath());
            try {
                String partialKey = computePartialKey(item.getPath(), item.getSizeBytes());
                byPartialKey.computeIfAbsent(partialKey, k -> new ArrayList<>()).add(item);
            } catch (IOException ex) {
                logManager.warn("Could not compute partial hash for: " + item.getPath() + " (" + ex.getMessage() + ")");
            }
        }

        List<SearchResultItem> fullHashCandidates = new ArrayList<>();
        for (List<SearchResultItem> group : byPartialKey.values()) {
            if (group.size() > 1) {
                fullHashCandidates.addAll(group);
            }
        }

        if (fullHashCandidates.isEmpty()) {
            logManager.info("Duplicate scan finished: no files matched partial hash.");
            return List.of();
        }

        if (algorithm == HashAlgorithm.PARTIAL_HASH) {
            for (Map.Entry<String, List<SearchResultItem>> entry : byPartialKey.entrySet()) {
                if (entry.getValue().size() > 1) {
                    for (SearchResultItem item : entry.getValue()) {
                        item.setDigest(entry.getKey());
                    }
                }
            }
            return buildGroupsFromMap(byPartialKey, cancelCheck);
        }

        // Stage 3: Full streaming digest (SHA-256 or MD5)
        String jcaAlgorithm = (algorithm == HashAlgorithm.MD5) ? "MD5" : "SHA-256";
        Map<String, List<SearchResultItem>> byFullDigest = new LinkedHashMap<>();
        total = fullHashCandidates.size();
        processed = 0L;

        for (SearchResultItem item : fullHashCandidates) {
            if (cancelCheck.getAsBoolean()) {
                return List.of();
            }
            processed++;
            publish(onProgress, jcaAlgorithm + " full hash", processed, total, item.getFullPath());
            try {
                String digest = computeFullDigest(item.getPath(), jcaAlgorithm, cancelCheck);
                if (digest == null) {
                    return List.of();
                }
                item.setDigest(digest);
                byFullDigest.computeIfAbsent(digest, k -> new ArrayList<>()).add(item);
            } catch (IOException | NoSuchAlgorithmException ex) {
                logManager.warn("Could not hash file: " + item.getPath() + " (" + ex.getMessage() + ")");
            }
        }

        return buildGroupsFromMap(byFullDigest, cancelCheck);
    }

    private List<SearchResultItem> collectFiles(
            List<Path> roots,
            boolean skipSymbolicLinks,
            boolean includeHidden,
            BooleanSupplier isCancelled,
            Consumer<DuplicateProgress> onProgress) {

        List<SearchResultItem> files = new ArrayList<>();
        EnumSet<FileVisitOption> options = skipSymbolicLinks
                ? EnumSet.noneOf(FileVisitOption.class)
                : EnumSet.of(FileVisitOption.FOLLOW_LINKS);

        for (Path root : roots) {
            if (isCancelled.getAsBoolean()) {
                return List.of();
            }
            if (!Files.exists(root)) {
                logManager.warn("Duplicate root does not exist, skipping: " + root);
                continue;
            }
            try {
                Files.walkFileTree(root, options, Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (isCancelled.getAsBoolean()) {
                            return FileVisitResult.TERMINATE;
                        }
                        if (!dir.equals(root) && shouldSkip(dir, attrs, skipSymbolicLinks, includeHidden)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isCancelled.getAsBoolean()) {
                            return FileVisitResult.TERMINATE;
                        }
                        if (!attrs.isRegularFile()) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (shouldSkip(file, attrs, skipSymbolicLinks, includeHidden)) {
                            return FileVisitResult.CONTINUE;
                        }
                        Instant mod = attrs.lastModifiedTime() != null ? attrs.lastModifiedTime().toInstant() : Instant.EPOCH;
                        files.add(new SearchResultItem(file, false, attrs.size(), mod));
                        if (files.size() % 500 == 0) {
                            publish(onProgress, "Collecting files", files.size(), 0L, file.toAbsolutePath().toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        logManager.warn("Skipped inaccessible file during duplicate scan: " + file);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                logManager.error("Error walking directory for duplicates: " + root, ex);
            }
        }
        if (isCancelled.getAsBoolean()) {
            return List.of();
        }
        publish(onProgress, "Collecting files", files.size(), files.size(), "");
        return files;
    }

    private boolean shouldSkip(Path entry, BasicFileAttributes attrs, boolean skipSymlinks, boolean includeHidden) {
        if (skipSymlinks && (attrs.isSymbolicLink() || Files.isSymbolicLink(entry))) {
            return true;
        }
        if (!includeHidden) {
            try {
                if (Files.isHidden(entry)) {
                    return true;
                }
            } catch (IOException ex) {
                logManager.warn("Could not read hidden attribute for: " + entry);
            }
        }
        return false;
    }

    private String computePartialKey(Path file, long sizeBytes) throws IOException {
        int chunkSize = AppConfig.PARTIAL_HASH_CHUNK_SIZE;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Long.toString(sizeBytes).getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                ByteBuffer headBuf = ByteBuffer.allocate((int) Math.min(chunkSize, sizeBytes));
                while (headBuf.hasRemaining() && channel.read(headBuf) > 0) {
                    // read head
                }
                headBuf.flip();
                md.update(headBuf);

                if (sizeBytes > chunkSize) {
                    long tailPos = Math.max(chunkSize, sizeBytes - chunkSize);
                    channel.position(tailPos);
                    ByteBuffer tailBuf = ByteBuffer.allocate((int) Math.min(chunkSize, sizeBytes - tailPos));
                    while (tailBuf.hasRemaining() && channel.read(tailBuf) > 0) {
                        // read tail
                    }
                    tailBuf.flip();
                    md.update(tailBuf);
                }
            }
            return sizeBytes + ":" + HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 unavailable", ex);
        }
    }

    private String computeFullDigest(Path file, String jcaAlgorithm, BooleanSupplier isCancelled)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(jcaAlgorithm);
        byte[] buffer = new byte[AppConfig.STREAM_BUFFER_SIZE];
        try (InputStream in = Files.newInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (isCancelled.getAsBoolean()) {
                    return null;
                }
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private List<DuplicateGroup> buildGroups(Map<Long, List<SearchResultItem>> bySize, String prefix, BooleanSupplier isCancelled) {
        List<DuplicateGroup> result = new ArrayList<>();
        for (Map.Entry<Long, List<SearchResultItem>> entry : bySize.entrySet()) {
            if (isCancelled.getAsBoolean()) {
                return List.of();
            }
            if (entry.getValue().size() > 1) {
                String key = prefix + ":" + entry.getKey();
                DuplicateGroup group = new DuplicateGroup(key, entry.getKey());
                entry.getValue().stream()
                        .sorted(Comparator.comparing(SearchResultItem::getFullPath))
                        .forEach(item -> {
                            item.setDigest(key);
                            group.addFile(item);
                        });
                result.add(group);
            }
        }
        result.sort(Comparator.comparingLong(DuplicateGroup::getReclaimableBytes).reversed());
        return result;
    }

    private List<DuplicateGroup> buildGroupsFromMap(Map<String, List<SearchResultItem>> grouped, BooleanSupplier isCancelled) {
        List<DuplicateGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<SearchResultItem>> entry : grouped.entrySet()) {
            if (isCancelled.getAsBoolean()) {
                return List.of();
            }
            List<SearchResultItem> files = entry.getValue();
            if (files.size() > 1) {
                long fileSize = files.get(0).getSizeBytes();
                DuplicateGroup group = new DuplicateGroup(entry.getKey(), fileSize);
                files.stream()
                        .sorted(Comparator.comparing(SearchResultItem::getFullPath))
                        .forEach(group::addFile);
                result.add(group);
            }
        }
        result.sort(Comparator.comparingLong(DuplicateGroup::getReclaimableBytes).reversed());
        return result;
    }

    private void publish(Consumer<DuplicateProgress> onProgress, String phase, long processed, long total, String file) {
        if (onProgress != null) {
            onProgress.accept(new DuplicateProgress(phase, processed, total, file));
        }
    }
}