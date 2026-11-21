package com.josegonzalezal.filesearcher.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Immutable representation of a matched file or directory entry.
 */
public final class SearchResultItem {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Path path;
    private final String name;
    private final String extension;
    private final boolean directory;
    private final long sizeBytes;
    private final Instant lastModified;
    private String digest;

    public SearchResultItem(Path path, boolean directory, long sizeBytes, Instant lastModified) {
        this.path = path;
        Path fileName = path.getFileName();
        this.name = (fileName != null) ? fileName.toString() : path.toString();
        this.directory = directory;
        this.sizeBytes = Math.max(0L, sizeBytes);
        this.lastModified = lastModified;
        this.extension = directory ? "" : extractExtension(this.name);
    }

    private static String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int unitIndex = -1;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }

    public Path getPath() {
        return path;
    }

    public String getFullPath() {
        return path.toAbsolutePath().toString();
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isDirectory() {
        return directory;
    }

    public String getTypeLabel() {
        if (Files.isSymbolicLink(path)) {
            return directory ? "Symlink Dir" : "Symlink File";
        }
        return directory ? "Folder" : "File";
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getFormattedSize() {
        return directory ? "-" : formatBytes(sizeBytes);
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public String getFormattedLastModified() {
        return (lastModified != null) ? DATE_FORMAT.format(lastModified) : "-";
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }
}