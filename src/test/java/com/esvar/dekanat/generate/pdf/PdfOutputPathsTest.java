package com.esvar.dekanat.generate.pdf;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfOutputPathsTest {

    @Test
    void resolveKeepsGeneratedFilesInsideUploads() {
        var path = PdfOutputPaths.resolve("../evil/name.pdf");

        assertEquals(Paths.get("uploads").resolve("evil_name.pdf"), path);
        assertFalse(path.toString().contains(".."));
    }

    @Test
    void partRemovesPathSeparatorsAndWhitespace() {
        assertEquals("group_name_1", PdfOutputPaths.part(" group/name 1 ", "group"));
    }

    @Test
    void fileNameLengthLimitPreservesPdfExtension() {
        String longName = "a".repeat(220) + ".pdf";

        String sanitized = PdfOutputPaths.fileName(longName);

        assertTrue(sanitized.length() <= 160);
        assertTrue(sanitized.endsWith(".pdf"));
    }
}
