package com.josegonzalezal.filesearcher.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportExportServiceTest {

    @TempDir
    Path dir;

    @Test
    void csvEscapesQuotesAndCommas() throws Exception {
        Path file = dir.resolve("sample, file.txt");
        Files.writeString(file, "payload");
        SearchResultItem item = new SearchResultItem(file, false, Files.size(file), Instant.EPOCH);

        Path csv = dir.resolve("out.csv");
        new ReportExportService(new LogManager(new AppConfig())).exportSearchResultsCsv(csv, List.of(item));

        String body = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(body.contains("\"sample, file.txt\""));
        assertTrue(body.contains("\"" + file.toAbsolutePath().toString().replace("\"", "\"\"") + "\""));
    }
}