package com.josegonzalezal.filesearcher.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ThemePreferences {

    private static final Path CONFIG_PATH = Path.of(
            System.getProperty("user.home"),
            ".file-searcher",
            "config.properties");
    private static final String KEY_DARK_MODE = "darkMode";

    public boolean isDarkMode() {
        if (!Files.exists(CONFIG_PATH)) {
            return false;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
            return Boolean.parseBoolean(props.getProperty(KEY_DARK_MODE, "false"));
        } catch (IOException ex) {
            return false;
        }
    }

    public void setDarkMode(boolean enabled) {
        Properties props = new Properties();
        props.setProperty(KEY_DARK_MODE, Boolean.toString(enabled));
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "File Searcher preferences");
            }
        } catch (IOException ignored) {
        }
    }
}