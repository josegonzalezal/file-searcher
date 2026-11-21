package com.josegonzalezal.filesearcher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.model.DuplicateGroup;
import com.josegonzalezal.filesearcher.model.HashAlgorithm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuplicateDetectorServiceTest {

    @TempDir
    Path dir;

    private DuplicateDetectorService service;

    @BeforeEach
    void setUp() {
        AppConfig config = new AppConfig();
        service = new DuplicateDetectorService(config, new LogManager(config));
    }

    @Test
    void sameSizeDifferentContentIsNotADuplicate() throws IOException {
        Files.writeString(dir.resolve("a.txt"), "hello");
        Files.writeString(dir.resolve("b.txt"), "hello");
        Files.writeString(dir.resolve("c.txt"), "world");

        List<DuplicateGroup> groups = service.findDuplicates(
                List.of(dir), HashAlgorithm.SHA_256, true, false, () -> false, null);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getFiles().size());
        assertFalse(groups.get(0).getFiles().stream()
                .anyMatch(item -> item.getPath().getFileName().toString().equals("c.txt")));
    }

    @Test
    void detectsDuplicatesAcrossSubdirectories() throws IOException {
        Path subDir = Files.createDirectory(dir.resolve("nested"));
        Files.writeString(dir.resolve("first.txt"), "duplicate-payload");
        Files.writeString(subDir.resolve("second.txt"), "duplicate-payload");

        List<DuplicateGroup> groups = service.findDuplicates(
                List.of(dir), HashAlgorithm.SHA_256, true, false, () -> false, null);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getFiles().size());
    }

    @Test
    void excludesHiddenFilesWhenIncludeHiddenIsFalse() throws IOException {
        Path hiddenFile = dir.resolve(".secret.txt");
        Files.writeString(hiddenFile, "duplicate-payload");
        Files.writeString(dir.resolve("visible.txt"), "duplicate-payload");
        Files.writeString(dir.resolve("other.txt"), "duplicate-payload");

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Files.setAttribute(hiddenFile, "dos:hidden", true);
        }

        List<DuplicateGroup> groups = service.findDuplicates(
                List.of(dir), HashAlgorithm.SHA_256, true, false, () -> false, null);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getFiles().size());
        assertFalse(groups.get(0).getFiles().stream()
                .anyMatch(item -> item.getPath().getFileName().toString().equals(".secret.txt")));
    }

    @Test
    void returnsEmptyListWhenNoDuplicatesExist() throws IOException {
        Files.writeString(dir.resolve("one.txt"), "alpha");
        Files.writeString(dir.resolve("two.txt"), "beta");

        List<DuplicateGroup> groups = service.findDuplicates(
                List.of(dir), HashAlgorithm.SHA_256, true, false, () -> false, null);

        assertTrue(groups.isEmpty());
    }

    @Test
    void stopsScanWhenCancellationRequested() throws IOException {
        Files.writeString(dir.resolve("a.txt"), "same");
        Files.writeString(dir.resolve("b.txt"), "same");
        AtomicBoolean cancelled = new AtomicBoolean(true);

        List<DuplicateGroup> groups = service.findDuplicates(
                List.of(dir), HashAlgorithm.SHA_256, true, false, cancelled::get, null);

        assertTrue(groups.isEmpty());
    }
}