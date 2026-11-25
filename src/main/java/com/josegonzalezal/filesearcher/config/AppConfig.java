package com.josegonzalezal.filesearcher.config;

import com.josegonzalezal.filesearcher.model.HashAlgorithm;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Holds portable runtime configuration for File Searcher.
 * Avoids generating temporary or junk files unless file logging is enabled.
 */
public final class AppConfig {

    public static final String APP_NAME = "File Searcher";
    public static final String VERSION = "1.0.7";
    public static final String AUTHOR_NAME = "José González Alonso";
    public static final String GITHUB_URL = "https://github.com/josegonzalezal/file-searcher";
    public static final int DEFAULT_DISPLAY_LIMIT = 10_000;
    public static final int STREAM_BUFFER_SIZE = 64 * 1024;
    public static final int PARTIAL_HASH_CHUNK_SIZE = 64 * 1024;

    private int maxDisplayedResults = DEFAULT_DISPLAY_LIMIT;
    private boolean enforceDisplayLimit = true;
    private boolean skipSymbolicLinks = true;
    private boolean includeHidden = false;
    private boolean writeLogFile = false;
    private HashAlgorithm defaultHashAlgorithm = HashAlgorithm.SHA_256;
    private Path logDirectory = Paths.get("logs");

    public int getMaxDisplayedResults() {
        return maxDisplayedResults;
    }

    public void setMaxDisplayedResults(int maxDisplayedResults) {
        if (maxDisplayedResults > 0) {
            this.maxDisplayedResults = maxDisplayedResults;
        }
    }

    public boolean isEnforceDisplayLimit() {
        return enforceDisplayLimit;
    }

    public void setEnforceDisplayLimit(boolean enforceDisplayLimit) {
        this.enforceDisplayLimit = enforceDisplayLimit;
    }

    public boolean isSkipSymbolicLinks() {
        return skipSymbolicLinks;
    }

    public void setSkipSymbolicLinks(boolean skipSymbolicLinks) {
        this.skipSymbolicLinks = skipSymbolicLinks;
    }

    public boolean isIncludeHidden() {
        return includeHidden;
    }

    public void setIncludeHidden(boolean includeHidden) {
        this.includeHidden = includeHidden;
    }

    public boolean isWriteLogFile() {
        return writeLogFile;
    }

    public void setWriteLogFile(boolean writeLogFile) {
        this.writeLogFile = writeLogFile;
    }

    public HashAlgorithm getDefaultHashAlgorithm() {
        return defaultHashAlgorithm;
    }

    public void setDefaultHashAlgorithm(HashAlgorithm defaultHashAlgorithm) {
        if (defaultHashAlgorithm != null) {
            this.defaultHashAlgorithm = defaultHashAlgorithm;
        }
    }

    public Path getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(Path logDirectory) {
        if (logDirectory != null) {
            this.logDirectory = logDirectory;
        }
    }
}