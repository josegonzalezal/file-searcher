package com.josegonzalezal.filesearcher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.josegonzalezal.filesearcher.model.DuplicateGroup;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import com.josegonzalezal.filesearcher.ui.MainFrame;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DuplicateDeletionSafetyTest {

    @TempDir
    Path tempDir;

    private SearchResultItem createItem(String fileName, long sizeBytes, String digest) {
        SearchResultItem item = new SearchResultItem(
                tempDir.resolve(fileName),
                false,
                sizeBytes,
                Instant.EPOCH);
        item.setDigest(digest);
        return item;
    }

    @Test
    void blocksDeletingAllFilesInDuplicateGroup() {
        SearchResultItem item1 = createItem("copy1.txt", 100L, "hash-a");
        SearchResultItem item2 = createItem("copy2.txt", 100L, "hash-a");

        List<String> blocked = MainFrame.findGroupsWithoutRemainingCopy(
                List.of(item1, item2),
                Set.of(0, 1));

        assertEquals(List.of("hash-a"), blocked);
    }

    @Test
    void allowsDeletingSubsetWhenAtLeastOneCopyRemains() {
        SearchResultItem item1 = createItem("copy1.txt", 100L, "hash-a");
        SearchResultItem item2 = createItem("copy2.txt", 100L, "hash-a");

        List<String> blocked = MainFrame.findGroupsWithoutRemainingCopy(
                List.of(item1, item2),
                Set.of(1));

        assertTrue(blocked.isEmpty());
    }

    @Test
    void duplicateGroupCalculatesReclaimableBytesLeavingOneCopy() {
        SearchResultItem item1 = createItem("a.bin", 2048L, "d1");
        SearchResultItem item2 = createItem("b.bin", 2048L, "d1");
        SearchResultItem item3 = createItem("c.bin", 2048L, "d1");

        DuplicateGroup group = new DuplicateGroup("d1", 2048L);
        group.addFile(item1);
        group.addFile(item2);
        group.addFile(item3);

        assertEquals(4096L, group.getReclaimableBytes());
        assertFalse(group.getFiles().isEmpty());
    }
}