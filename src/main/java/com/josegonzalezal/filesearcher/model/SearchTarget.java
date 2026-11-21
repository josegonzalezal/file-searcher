package com.josegonzalezal.filesearcher.model;

public enum SearchTarget {
    FILES("Files only"),
    FOLDERS("Folders only"),
    BOTH("Files and folders");

    private final String label;

    SearchTarget(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}