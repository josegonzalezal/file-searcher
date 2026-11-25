package com.josegonzalezal.filesearcher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileActionServiceTest {

    @TempDir
    Path dir;

    @Test
    void refusesToDeleteDirectories() throws Exception {
        Path nested = Files.createDirectory(dir.resolve("keep-me"));
        Files.writeString(nested.resolve("child.txt"), "data");
        Path file = dir.resolve("remove-me.txt");
        Files.writeString(file, "gone");

        FileActionService service = new FileActionService(new LogManager(new AppConfig()));
        FileActionService.BatchOperationResult result = service.deletePathsPermanently(List.of(nested, file));

        assertEquals(1, result.succeeded());
        assertEquals(1, result.errors().size());
        assertTrue(Files.exists(nested));
        assertTrue(Files.exists(nested.resolve("child.txt")));
        assertTrue(Files.notExists(file));
    }
}
