package com.josegonzalezal.filesearcher.model;

public enum MatchMode {
    CONTAINS("Name contains"),
    EXACT("Exact name"),
    REGEX("Regular expression");

    private final String label;

    MatchMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}