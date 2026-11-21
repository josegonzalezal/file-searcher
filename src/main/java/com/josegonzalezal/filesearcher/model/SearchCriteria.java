package com.josegonzalezal.filesearcher.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Encapsulates validated search roots and filter rules.
 */
public final class SearchCriteria {

    private final List<Path> roots;
    private final SearchTarget target;
    private final String nameQuery;
    private final MatchMode matchMode;
    private final boolean caseSensitive;
    private final Pattern compiledRegex;
    private final Set<String> allowedExtensions;
    private final Long minSizeBytes;
    private final Long maxSizeBytes;
    private final boolean skipSymbolicLinks;
    private final boolean includeHidden;

    private SearchCriteria(Builder builder) {
        this.roots = Collections.unmodifiableList(builder.roots);
        this.target = builder.target;
        this.nameQuery = builder.nameQuery;
        this.matchMode = builder.matchMode;
        this.caseSensitive = builder.caseSensitive;
        this.compiledRegex = builder.compiledRegex;
        this.allowedExtensions = Collections.unmodifiableSet(builder.allowedExtensions);
        this.minSizeBytes = builder.minSizeBytes;
        this.maxSizeBytes = builder.maxSizeBytes;
        this.skipSymbolicLinks = builder.skipSymbolicLinks;
        this.includeHidden = builder.includeHidden;
    }

    public List<Path> getRoots() {
        return roots;
    }

    public SearchTarget getTarget() {
        return target;
    }

    public String getNameQuery() {
        return nameQuery;
    }

    public MatchMode getMatchMode() {
        return matchMode;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public Long getMinSizeBytes() {
        return minSizeBytes;
    }

    public Long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public boolean isSkipSymbolicLinks() {
        return skipSymbolicLinks;
    }

    public boolean isIncludeHidden() {
        return includeHidden;
    }

    public boolean matchesTargetType(boolean isDirectory) {
        return switch (target) {
            case FILES -> !isDirectory;
            case FOLDERS -> isDirectory;
            case BOTH -> true;
        };
    }

    public boolean matchesName(String entryName) {
        if (nameQuery == null || nameQuery.isEmpty()) {
            return true;
        }
        return switch (matchMode) {
            case CONTAINS -> {
                if (caseSensitive) {
                    yield entryName.contains(nameQuery);
                }
                yield entryName.toLowerCase(Locale.ROOT).contains(nameQuery.toLowerCase(Locale.ROOT));
            }
            case EXACT -> {
                if (caseSensitive) {
                    yield entryName.equals(nameQuery);
                }
                yield entryName.equalsIgnoreCase(nameQuery);
            }
            case REGEX -> compiledRegex != null && compiledRegex.matcher(entryName).matches();
        };
    }

    public boolean matchesExtension(String extension, boolean isDirectory) {
        if (allowedExtensions.isEmpty()) {
            return true;
        }
        if (isDirectory) {
            return false;
        }
        return allowedExtensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    public boolean matchesSize(long sizeBytes, boolean isDirectory) {
        if (isDirectory) {
            return minSizeBytes == null && maxSizeBytes == null;
        }
        if (minSizeBytes != null && sizeBytes < minSizeBytes) {
            return false;
        }
        if (maxSizeBytes != null && sizeBytes > maxSizeBytes) {
            return false;
        }
        return true;
    }

    public static final class Builder {
        private List<Path> roots = List.of();
        private SearchTarget target = SearchTarget.BOTH;
        private String nameQuery = "";
        private MatchMode matchMode = MatchMode.CONTAINS;
        private boolean caseSensitive = false;
        private Pattern compiledRegex;
        private Set<String> allowedExtensions = Set.of();
        private Long minSizeBytes;
        private Long maxSizeBytes;
        private boolean skipSymbolicLinks = true;
        private boolean includeHidden = false;

        public Builder roots(List<Path> roots) {
            this.roots = roots != null ? roots : List.of();
            return this;
        }

        public Builder target(SearchTarget target) {
            this.target = target != null ? target : SearchTarget.BOTH;
            return this;
        }

        public Builder nameQuery(String nameQuery) {
            this.nameQuery = nameQuery != null ? nameQuery.trim() : "";
            return this;
        }

        public Builder matchMode(MatchMode matchMode) {
            this.matchMode = matchMode != null ? matchMode : MatchMode.CONTAINS;
            return this;
        }

        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public Builder extensionsCsv(String rawExtensions) {
            if (rawExtensions == null || rawExtensions.isBlank()) {
                this.allowedExtensions = Set.of();
                return this;
            }
            this.allowedExtensions = rawExtensions.lines()
                    .flatMap(line -> List.of(line.split("[,;\\s]+")).stream())
                    .map(ext -> ext.trim().replaceFirst("^\\.+", "").toLowerCase(Locale.ROOT))
                    .filter(ext -> !ext.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
            return this;
        }

        public Builder minSizeBytes(Long minSizeBytes) {
            this.minSizeBytes = minSizeBytes;
            return this;
        }

        public Builder maxSizeBytes(Long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        public Builder skipSymbolicLinks(boolean skipSymbolicLinks) {
            this.skipSymbolicLinks = skipSymbolicLinks;
            return this;
        }

        public Builder includeHidden(boolean includeHidden) {
            this.includeHidden = includeHidden;
            return this;
        }

        public SearchCriteria build() {
            if (roots.isEmpty()) {
                throw new IllegalArgumentException("At least one valid search root is required.");
            }
            if (minSizeBytes != null && minSizeBytes < 0) {
                throw new IllegalArgumentException("Minimum file size cannot be negative.");
            }
            if (maxSizeBytes != null && maxSizeBytes < 0) {
                throw new IllegalArgumentException("Maximum file size cannot be negative.");
            }
            if (minSizeBytes != null && maxSizeBytes != null && minSizeBytes > maxSizeBytes) {
                throw new IllegalArgumentException("Minimum file size cannot exceed maximum file size.");
            }
            if (matchMode == MatchMode.REGEX && !nameQuery.isEmpty()) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                this.compiledRegex = Pattern.compile(nameQuery, flags);
            }
            return new SearchCriteria(this);
        }
    }
}