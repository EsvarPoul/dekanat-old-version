package com.esvar.dekanat.generate.pdf;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

final class PdfOutputPaths {

    private static final int MAX_FILE_NAME_LENGTH = 160;

    private PdfOutputPaths() {
    }

    static Path resolve(String fileName) {
        return Paths.get("uploads").resolve(fileName(fileName));
    }

    static String part(Object value, String fallback) {
        return sanitize(Objects.toString(value, ""), fallback, false);
    }

    static String fileName(String value) {
        String cleaned = sanitize(value, "document.pdf", true);
        if (cleaned.length() <= MAX_FILE_NAME_LENGTH) {
            return cleaned;
        }

        int dot = cleaned.lastIndexOf('.');
        String extension = dot > 0 ? cleaned.substring(dot) : "";
        String stem = dot > 0 ? cleaned.substring(0, dot) : cleaned;
        int stemLimit = Math.max(1, MAX_FILE_NAME_LENGTH - extension.length());
        return stem.substring(0, Math.min(stem.length(), stemLimit)) + extension;
    }

    private static String sanitize(String value, String fallback, boolean allowDot) {
        String text = Objects.toString(value, "").trim();
        text = text.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_");
        text = text.replaceAll("\\s+", "_");
        if (!allowDot) {
            text = text.replace('.', '_');
        } else {
            text = text.replaceAll("\\.{2,}", ".");
        }
        text = text.replaceAll("_+", "_");
        text = text.replaceAll("^[._-]+|[._-]+$", "");

        if (text.isBlank()) {
            text = Objects.toString(fallback, "document").trim();
        }
        if (text.isBlank()) {
            text = "document";
        }
        return text.toLowerCase(Locale.ROOT).equals("con") ? "document" : text;
    }
}
