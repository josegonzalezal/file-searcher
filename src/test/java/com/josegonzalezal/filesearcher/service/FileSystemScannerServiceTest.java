package com.josegonzalezal.filesearcher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.model.SearchCriteria;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import com.josegonzalezal.filesearcher.model.SearchTarget;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemScannerServiceTest {

    @TempDir
    Path dir;

    @Test
    void skipSymlinksDoesNotFollowLinkedDirectory() throws Exception {
        Path realDir = Files.createDirectory(dir.resolve("real"));
        Files.writeString(realDir.resolve("inside.txt"), "payload");
        Path link = dir.resolve("alias");
        try {
            Files.createSymbolicLink(link, realDir);
        } catch (Exception ex) {
            assumeTrue(false, "Symbolic links are not available: " + ex.getMessage());
        }

        SearchCriteria criteria = new SearchCriteria.Builder()
                .roots(List.of(dir))
                .target(SearchTarget.FILES)
                .skipSymbolicLinks(true)
                .build();

        List<SearchResultItem> matches = new ArrayList<>();
        new FileSystemScannerService(new LogManager(new AppConfig())).scan(
                criteria,
                100,
                true,
                () -> false,
                matches::add,
                null);

        long insideMatches = matches.stream()
                .filter(item -> item.getName().equals("inside.txt"))
                .count();
        assertEquals(1, insideMatches);
    }
}
