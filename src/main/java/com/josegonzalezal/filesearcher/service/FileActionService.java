package com.josegonzalezal.filesearcher.service;

import com.josegonzalezal.filesearcher.config.LogManager;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Performs safe OS integrations and explicit user-confirmed file/directory operations.
 */
public final class FileActionService {

    public record BatchOperationResult(int succeeded, List<String> errors) {
    }

    private final LogManager logManager;

    public FileActionService(LogManager logManager) {
        this.logManager = logManager;
    }

    public void openFile(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("Target does not exist: " + path);
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop actions are not supported on this platform.");
        }
        Desktop desktop = Desktop.getDesktop();
        if (Files.isDirectory(path)) {
            desktop.open(path.toFile());
        } else if (desktop.isSupported(Desktop.Action.OPEN)) {
            desktop.open(path.toFile());
        } else {
            throw new IOException("OPEN action is not supported for: " + path);
        }
        logManager.info("Opened target: " + path);
    }

    public void openContainingFolder(Path path) throws IOException {
        if (path == null) {
            throw new IOException("Path is null.");
        }
        Path parent = Files.isDirectory(path) ? path : path.getParent();
        if (parent == null || !Files.exists(parent)) {
            throw new IOException("Containing folder does not exist: " + parent);
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new IOException("Opening folders is not supported on this platform.");
        }
        Desktop.getDesktop().open(parent.toFile());
        logManager.info("Opened containing folder: " + parent);
    }

    public BatchOperationResult deletePathsPermanently(List<Path> paths) {
        int succeeded = 0;
        List<String> errors = new ArrayList<>();

        for (Path target : paths) {
            try {
                if (!Files.exists(target, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                    errors.add(target + " (does not exist)");
                    continue;
                }
                if (Files.isDirectory(target, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                    deleteDirectoryRecursively(target);
                } else {
                    Files.delete(target);
                }
                succeeded++;
                logManager.info("Permanently deleted: " + target);
            } catch (IOException | SecurityException ex) {
                String msg = target + " (" + ex.getMessage() + ")";
                errors.add(msg);
                logManager.error("Failed to delete: " + target, ex);
            }
        }

        return new BatchOperationResult(succeeded, errors);
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> sorted = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : sorted) {
                Files.delete(p);
            }
        }
    }
}