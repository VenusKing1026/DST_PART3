package cn.edu.zju.service;

import java.util.*;

public class AnnovarValidator {

    public static void validateMultianno(String content) {
        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) {
            throw new IllegalArgumentException("ANNOVAR output is empty");
        }

        String[] headers = lines[0].split("\\t", -1);
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i], i);
        }

        List<String> required = Arrays.asList("Chr", "Start", "End", "Ref", "Alt");
        for (String field : required) {
            if (!headerIndex.containsKey(field)) {
                throw new IllegalArgumentException("Missing required ANNOVAR column: " + field);
            }
        }

        boolean hasValidRow = false;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }

            if (line.startsWith("##") || line.startsWith("#CHROM")) {
                throw new IllegalArgumentException("ANNOVAR output contains raw VCF header lines");
            }

            String[] split = line.split("\\t", -1);

            String chr = getValue(split, headerIndex, "Chr");
            String start = getValue(split, headerIndex, "Start");
            String end = getValue(split, headerIndex, "End");
            String ref = getValue(split, headerIndex, "Ref");
            String alt = getValue(split, headerIndex, "Alt");

            if (chr.isBlank() || !start.matches("\\d+") || !end.matches("\\d+")
                    || ref.isBlank() || alt.isBlank()) {
                throw new IllegalArgumentException("Invalid ANNOVAR row at line " + (i + 1) + ": " + line);
            }

            hasValidRow = true;
        }

        if (!hasValidRow) {
            throw new IllegalArgumentException("No valid ANNOVAR rows found");
        }
    }

    private static String getValue(String[] split, Map<String, Integer> headerIndex, String column) {
        Integer idx = headerIndex.get(column);
        if (idx == null || idx >= split.length) {
            return "";
        }
        String value = split[idx] == null ? "" : split[idx].trim();
        return ".".equals(value) ? "" : value;
    }
}