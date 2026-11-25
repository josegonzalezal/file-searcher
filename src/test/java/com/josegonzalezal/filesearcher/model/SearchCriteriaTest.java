package com.josegonzalezal.filesearcher.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SearchCriteriaTest {

    @TempDir
    Path root;

    private SearchCriteria.Builder base() {
        return new SearchCriteria.Builder().roots(List.of(root));
    }

    @Test
    void wildcardMatchesSingleCharacterAndStar() {
        SearchCriteria criteria = base()
                .matchMode(MatchMode.WILDCARD)
                .nameQuery("report-?.pdf")
                .build();

        assertTrue(criteria.matchesName("report-1.pdf"));
        assertFalse(criteria.matchesName("report-10.pdf"));
        assertFalse(criteria.matchesName("invoice-1.pdf"));
    }

    @Test
    void regexMatchesAlternateTerms() {
        SearchCriteria criteria = base()
                .matchMode(MatchMode.REGEX)
                .nameQuery("invoice|receipt")
                .build();

        assertTrue(criteria.matchesName("invoice"));
        assertTrue(criteria.matchesName("receipt"));
        assertFalse(criteria.matchesName("contract"));
    }

    @Test
    void literalModePreservesSpacesAndIgnoresWildcardSyntax() {
        SearchCriteria criteria = base()
                .matchMode(MatchMode.WILDCARD)
                .literalName(true)
                .nameQuery("  file*.txt  ")
                .build();

        assertTrue(criteria.matchesName("prefix  file*.txt  suffix"));
        assertFalse(criteria.matchesName("file1.txt"));
    }
}
