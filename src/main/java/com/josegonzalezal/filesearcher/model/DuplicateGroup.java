package com.josegonzalezal.filesearcher.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups files that match under the selected duplicate detection strategy.
 */
public final class DuplicateGroup {

    private final String groupKey;
    private final long fileSizeBytes;
    private final List<SearchResultItem> files = new ArrayList<>();

    public DuplicateGroup(String groupKey, long fileSizeBytes) {
        this.groupKey = groupKey;
        this.fileSizeBytes = Math.max(0L, fileSizeBytes);
    }

    public void addFile(SearchResultItem item) {
        files.add(item);
    }

    public String getGroupKey() {
        return groupKey;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFormattedFileSize() {
        return SearchResultItem.formatBytes(fileSizeBytes);
    }

    public List<SearchResultItem> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public long getReclaimableBytes() {
        if (files.size() <= 1) {
            return 0L;
        }
        return fileSizeBytes * (files.size() - 1L);
    }

    public String getFormattedReclaimableSpace() {
        return SearchResultItem.formatBytes(getReclaimableBytes());
    }
}