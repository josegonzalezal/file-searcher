package com.josegonzalezal.filesearcher.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.josegonzalezal.filesearcher.model.MatchMode;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainFrameDuplicateFilterTest {

    @TempDir
    Path tempDir;

    private SearchResultItem createItem(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content);
        return new SearchResultItem(file, false, Files.size(file), Instant.EPOCH);
    }

    @Test
    void parseExtensionsInputSupportsCommasSemicolonsSpacesAndLeadingDots() {
        Set<String> parsed = MainFrame.parseExtensionsInput("  .PDF, jpg;  .TXT   png , ; ");
        assertEquals(Set.of("pdf", "jpg", "txt", "png"), parsed);
    }

    @Test
    void parseExtensionsInputReturnsEmptySetForBlankOrNull() {
        assertTrue(MainFrame.parseExtensionsInput(null).isEmpty());
        assertTrue(MainFrame.parseExtensionsInput("   ; ,  ").isEmpty());
    }

    @Test
    void matchesDuplicateFiltersFiltersBySizeAndExtensions() throws IOException {
        SearchResultItem doc = createItem("notes.txt", "123456"); // 6 bytes
        Set<String> exts = MainFrame.parseExtensionsInput("txt, md");

        assertTrue(MainFrame.matchesDuplicateFilters(
                doc, 4L, 10L, exts, "", null, MatchMode.CONTAINS, false, false));
        assertFalse(MainFrame.matchesDuplicateFilters(
                doc, 8L, 10L, exts, "", null, MatchMode.CONTAINS, false, false));
        assertFalse(MainFrame.matchesDuplicateFilters(
                doc, 1L, 5L, exts, "", null, MatchMode.CONTAINS, false, false));
        assertFalse(MainFrame.matchesDuplicateFilters(
                doc, 1L, 10L, Set.of("pdf"), "", null, MatchMode.CONTAINS, false, false));
    }

    @Test
    void matchesDuplicateFiltersRespectsMatchModesCaseSensitivityAndLiteralMode() throws IOException {
        SearchResultItem report = createItem("Quarterly_Report_2026.PDF", "payload");

        // EXACT & case-insensitive vs case-sensitive
        assertTrue(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), "quarterly_report_2026.pdf",
                null, MatchMode.EXACT, false, false));
        assertFalse(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), "quarterly_report_2026.pdf",
                null, MatchMode.EXACT, true, false));

        // STARTS_WITH and ENDS_WITH
        assertTrue(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), "Quarterly_",
                null, MatchMode.STARTS_WITH, true, false));
        assertTrue(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), ".PDF",
                null, MatchMode.ENDS_WITH, true, false));

        // Compiled pattern (e.g. regex or wildcard)
        Pattern regex = Pattern.compile("^Quarterly_.*_2026\\.PDF$");
        assertTrue(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), "Quarterly_.*_2026\\.PDF",
                regex, MatchMode.REGEX, true, false));

        // Literal mode checks substring match with case sensitivity
        assertTrue(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), "Report_2026",
                null, MatchMode.EXACT, true, true));
        assertFalse(MainFrame.matchesDuplicateFilters(
                report, null, null, Set.of(), "report_2026",
                null, MatchMode.EXACT, true, true));
    }
}