package com.josegonzalezal.filesearcher.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserPreferencesTest {

    @TempDir
    Path dir;

    @Test
    void saveMergesDarkModeWithoutDroppingOtherKeys() {
        Path file = dir.resolve("config.properties");
        UserPreferences prefs = new UserPreferences(file);
        prefs.save(new UserPreferences.SessionState(
                false,
                List.of("C:\\data"),
                "report",
                "CONTAINS",
                false,
                "FILES",
                "pdf",
                "1KB",
                "",
                true,
                false,
                true,
                true,
                "SHA_256",
                "Find Files / Folders"));

        prefs.setDarkMode(true);
        UserPreferences.SessionState loaded = prefs.load();
        assertTrue(loaded.darkMode());
        assertEquals(List.of("C:\\data"), loaded.roots());
        assertEquals("report", loaded.namePattern());
        assertEquals("SHA_256", loaded.hashAlgorithm());
    }
}
