package com.josegonzalezal.filesearcher.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Thread-safe Swing logger that feeds the GUI log area and optionally appends to a log file.
 */
public final class LogManager {

    public enum Level {
        INFO,
        WARN,
        ERROR
    }

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_MEMORY_ENTRIES = 5_000;

    private final AppConfig config;
    private final List<String> logEntries = new ArrayList<>();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final Object memoryLock = new Object();
    private final Object fileLock = new Object();

    public LogManager(AppConfig config) {
        this.config = config;
    }

    public List<String> getLogEntriesSnapshot() {
        synchronized (memoryLock) {
            return Collections.unmodifiableList(new ArrayList<>(logEntries));
        }
    }

    public void addLogListener(Consumer<String> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeLogListener(Consumer<String> listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void info(String message) {
        log(Level.INFO, message, null);
    }

    public void warn(String message) {
        log(Level.WARN, message, null);
    }

    public void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    public void log(Level level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String detail = (throwable != null && throwable.getMessage() != null)
                ? " | " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
                : (throwable != null ? " | " + throwable.getClass().getSimpleName() : "");
        String formatted = String.format("[%s] [%s] %s%s", timestamp, level, message, detail);

        synchronized (memoryLock) {
            if (logEntries.size() >= MAX_MEMORY_ENTRIES) {
                logEntries.remove(0);
            }
            logEntries.add(formatted);
        }

        Runnable notifyListeners = () -> {
            for (Consumer<String> listener : listeners) {
                listener.accept(formatted);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            notifyListeners.run();
        } else {
            SwingUtilities.invokeLater(notifyListeners);
        }

        if (config != null && config.isWriteLogFile()) {
            appendToFile(formatted);
        }
    }

    private void appendToFile(String formatted) {
        synchronized (fileLock) {
            try {
                Path dir = config.getLogDirectory();
                Files.createDirectories(dir);
                Path logFile = dir.resolve("file-searcher-" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".log");
                try (BufferedWriter writer = Files.newBufferedWriter(
                        logFile,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND)) {
                    writer.write(formatted);
                    writer.newLine();
                }
            } catch (IOException ex) {
                String errLine = "[" + LocalDateTime.now().format(TIME_FORMAT)
                        + "] [ERROR] Failed writing log file: " + ex.getMessage();
                synchronized (memoryLock) {
                    if (logEntries.size() >= MAX_MEMORY_ENTRIES) {
                        logEntries.remove(0);
                    }
                    logEntries.add(errLine);
                }
                Runnable notifyErr = () -> {
                    for (Consumer<String> listener : listeners) {
                        listener.accept(errLine);
                    }
                };
                if (SwingUtilities.isEventDispatchThread()) {
                    notifyErr.run();
                } else {
                    SwingUtilities.invokeLater(notifyErr);
                }
            }
        }
    }
}
