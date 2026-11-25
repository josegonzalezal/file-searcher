package com.josegonzalezal.filesearcher.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashAlgorithmTest {

    @Test
    void permanentDeletionRequiresFullContentHash() {
        assertFalse(HashAlgorithm.SIZE_ONLY.allowsPermanentDeletion());
        assertFalse(HashAlgorithm.PARTIAL_HASH.allowsPermanentDeletion());
        assertTrue(HashAlgorithm.SHA_256.allowsPermanentDeletion());
        assertTrue(HashAlgorithm.MD5.allowsPermanentDeletion());
    }
}
