package com.josegonzalezal.filesearcher.service;

import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.model.DuplicateGroup;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Exports search results or duplicate groups to UTF-8 CSV or plain text reports.
 */
public final class ReportExportService {

    private final LogManager logManager;

    public ReportExportService(LogManager logManager) {
        this.logManager = logManager;
    }

    public void exportSearchResultsCsv(Path outputFile, List<SearchResultItem> items) throws IOException {
        ensureParentDirectory(outputFile);
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("Name,Type,Extension,SizeBytes,FormattedSize,LastModified,FullPath");
            writer.newLine();
            for (SearchResultItem item : items) {
                writer.write(String.join(",",
                        escapeCsv(item.getName()),
                        escapeCsv(item.getTypeLabel()),
                        escapeCsv(item.getExtension()),
                        Long.toString(item.getSizeBytes()),
                        escapeCsv(item.getFormattedSize()),
                        escapeCsv(item.getFormattedLastModified()),
                        escapeCsv(item.getFullPath())));
                writer.newLine();
            }
        }
        logManager.info("Exported " + items.size() + " search results to CSV: " + outputFile);
    }

    public void exportSearchResultsTxt(Path outputFile, List<SearchResultItem> items) throws IOException {
        ensureParentDirectory(outputFile);
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("File Searcher - Search Results Report");
            writer.newLine();
            writer.write("Total entries: " + items.size());
            writer.newLine();
            writer.write("========================================");
            writer.newLine();
            for (SearchResultItem item : items) {
                writer.write(String.format("[%s] %s | Size: %s | Modified: %s | Path: %s",
                        item.getTypeLabel(),
                        item.getName(),
                        item.getFormattedSize(),
                        item.getFormattedLastModified(),
                        item.getFullPath()));
                writer.newLine();
            }
        }
        logManager.info("Exported " + items.size() + " search results to TXT: " + outputFile);
    }

    public void exportDuplicatesCsv(Path outputFile, List<DuplicateGroup> groups) throws IOException {
        ensureParentDirectory(outputFile);
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("GroupKey,FileSizeBytes,ReclaimableBytes,FileName,LastModified,FullPath");
            writer.newLine();
            for (DuplicateGroup group : groups) {
                for (SearchResultItem file : group.getFiles()) {
                    writer.write(String.join(",",
                            escapeCsv(group.getGroupKey()),
                            Long.toString(group.getFileSizeBytes()),
                            Long.toString(group.getReclaimableBytes()),
                            escapeCsv(file.getName()),
                            escapeCsv(file.getFormattedLastModified()),
                            escapeCsv(file.getFullPath())));
                    writer.newLine();
                }
            }
        }
        logManager.info("Exported " + groups.size() + " duplicate groups to CSV: " + outputFile);
    }

    private void ensureParentDirectory(Path outputFile) throws IOException {
        Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String sanitized = value.replace("\"", "\"\"");
        return "\"" + sanitized + "\"";
    }
}