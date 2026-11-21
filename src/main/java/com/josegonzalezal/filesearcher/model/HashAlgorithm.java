package com.josegonzalezal.filesearcher.model;

public enum HashAlgorithm {
    SIZE_ONLY("File size only (fastest)"),
    PARTIAL_HASH("Size + Head/Tail hash (fast)"),
    SHA_256("SHA-256 full file (streaming)"),
    MD5("MD5 full file (streaming)");

    private final String label;

    HashAlgorithm(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}