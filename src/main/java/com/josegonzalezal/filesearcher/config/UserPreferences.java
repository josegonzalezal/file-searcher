package com.josegonzalezal.filesearcher.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Loads and stores UI session preferences in {@code ~/.file-searcher/config.properties}.
 */
public final class UserPreferences {

    public record SessionState(
            boolean darkMode,
            List<String> roots,
            String namePattern,
            String matchMode,
            boolean literalName,
            String searchTarget,
            String extensions,
            String minSize,
            String maxSize,
            boolean caseSensitive,
            boolean includeHidden,
            boolean skipSymlinks,
            boolean capResults,
            String hashAlgorithm,
            String mode,
            Integer windowWidth,
            Integer windowHeight,
            Integer windowX,
            Integer windowY) {

        public SessionState(
                boolean darkMode,
                List<String> roots,
                String namePattern,
                String matchMode,
                boolean literalName,
                String searchTarget,
                String extensions,
                String minSize,
                String maxSize,
                boolean caseSensitive,
                boolean includeHidden,
                boolean skipSymlinks,
                boolean capResults,
                String hashAlgorithm,
                String mode) {
            this(
                    darkMode,
                    roots,
                    namePattern,
                    matchMode,
                    literalName,
                    searchTarget,
                    extensions,
                    minSize,
                    maxSize,
                    caseSensitive,
                    includeHidden,
                    skipSymlinks,
                    capResults,
                    hashAlgorithm,
                    mode,
                    null,
                    null,
                    null,
                    null);
        }
    }

    private static final String KEY_DARK_MODE = "darkMode";
    private static final String KEY_ROOTS = "roots";
    private static final String KEY_NAME_PATTERN = "namePattern";
    private static final String KEY_MATCH_MODE = "matchMode";
    private static final String KEY_LITERAL = "literalName";
    private static final String KEY_TARGET = "searchTarget";
    private static final String KEY_EXTENSIONS = "extensions";
    private static final String KEY_MIN_SIZE = "minSize";
    private static final String KEY_MAX_SIZE = "maxSize";
    private static final String KEY_CASE_SENSITIVE = "caseSensitive";
    private static final String KEY_INCLUDE_HIDDEN = "includeHidden";
    private static final String KEY_SKIP_SYMLINKS = "skipSymlinks";
    private static final String KEY_CAP_RESULTS = "capResults";
    private static final String KEY_HASH_ALGORITHM = "hashAlgorithm";
    private static final String KEY_MODE = "mode";
    private static final String KEY_WINDOW_WIDTH = "window.width";
    private static final String KEY_WINDOW_HEIGHT = "window.height";
    private static final String KEY_WINDOW_X = "window.x";
    private static final String KEY_WINDOW_Y = "window.y";

    private final Path configPath;

    public UserPreferences() {
        this(Path.of(System.getProperty("user.home"), ".file-searcher", "config.properties"));
    }

    public UserPreferences(Path configPath) {
        this.configPath = configPath;
    }

    public SessionState load() {
        Properties props = loadProperties();
        return new SessionState(
                Boolean.parseBoolean(props.getProperty(KEY_DARK_MODE, "false")),
                parseRoots(props.getProperty(KEY_ROOTS, "")),
                props.getProperty(KEY_NAME_PATTERN, ""),
                props.getProperty(KEY_MATCH_MODE, ""),
                Boolean.parseBoolean(props.getProperty(KEY_LITERAL, "false")),
                props.getProperty(KEY_TARGET, ""),
                props.getProperty(KEY_EXTENSIONS, ""),
                props.getProperty(KEY_MIN_SIZE, ""),
                props.getProperty(KEY_MAX_SIZE, ""),
                Boolean.parseBoolean(props.getProperty(KEY_CASE_SENSITIVE, "false")),
                Boolean.parseBoolean(props.getProperty(KEY_INCLUDE_HIDDEN, "false")),
                Boolean.parseBoolean(props.getProperty(KEY_SKIP_SYMLINKS, "true")),
                Boolean.parseBoolean(props.getProperty(KEY_CAP_RESULTS, "true")),
                props.getProperty(KEY_HASH_ALGORITHM, ""),
                props.getProperty(KEY_MODE, ""),
                parsePositiveInt(props.getProperty(KEY_WINDOW_WIDTH)),
                parsePositiveInt(props.getProperty(KEY_WINDOW_HEIGHT)),
                parseInt(props.getProperty(KEY_WINDOW_X)),
                parseInt(props.getProperty(KEY_WINDOW_Y)));
    }

    public void save(SessionState state) {
        if (state == null) {
            return;
        }
        Properties props = loadProperties();
        props.setProperty(KEY_DARK_MODE, Boolean.toString(state.darkMode()));
        props.setProperty(KEY_ROOTS, String.join("|", state.roots()));
        props.setProperty(KEY_NAME_PATTERN, nullToEmpty(state.namePattern()));
        props.setProperty(KEY_MATCH_MODE, nullToEmpty(state.matchMode()));
        props.setProperty(KEY_LITERAL, Boolean.toString(state.literalName()));
        props.setProperty(KEY_TARGET, nullToEmpty(state.searchTarget()));
        props.setProperty(KEY_EXTENSIONS, nullToEmpty(state.extensions()));
        props.setProperty(KEY_MIN_SIZE, nullToEmpty(state.minSize()));
        props.setProperty(KEY_MAX_SIZE, nullToEmpty(state.maxSize()));
        props.setProperty(KEY_CASE_SENSITIVE, Boolean.toString(state.caseSensitive()));
        props.setProperty(KEY_INCLUDE_HIDDEN, Boolean.toString(state.includeHidden()));
        props.setProperty(KEY_SKIP_SYMLINKS, Boolean.toString(state.skipSymlinks()));
        props.setProperty(KEY_CAP_RESULTS, Boolean.toString(state.capResults()));
        props.setProperty(KEY_HASH_ALGORITHM, nullToEmpty(state.hashAlgorithm()));
        props.setProperty(KEY_MODE, nullToEmpty(state.mode()));
        setOptionalPositiveInt(props, KEY_WINDOW_WIDTH, state.windowWidth());
        setOptionalPositiveInt(props, KEY_WINDOW_HEIGHT, state.windowHeight());
        setOptionalInt(props, KEY_WINDOW_X, state.windowX());
        setOptionalInt(props, KEY_WINDOW_Y, state.windowY());
        store(props);
    }

    public void setDarkMode(boolean enabled) {
        Properties props = loadProperties();
        props.setProperty(KEY_DARK_MODE, Boolean.toString(enabled));
        store(props);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        if (!Files.exists(configPath)) {
            return props;
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        } catch (IOException ignored) {
        }
        return props;
    }

    private void store(Properties props) {
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "File Searcher preferences");
            }
        } catch (IOException ignored) {
        }
    }

    private static List<String> parseRoots(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parsePositiveInt(String raw) {
        Integer value = parseInt(raw);
        return (value != null && value > 0) ? value : null;
    }

    private static void setOptionalInt(Properties props, String key, Integer value) {
        if (value != null) {
            props.setProperty(key, Integer.toString(value));
        } else {
            props.remove(key);
        }
    }

    private static void setOptionalPositiveInt(Properties props, String key, Integer value) {
        if (value != null && value > 0) {
            props.setProperty(key, Integer.toString(value));
        } else {
            props.remove(key);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}