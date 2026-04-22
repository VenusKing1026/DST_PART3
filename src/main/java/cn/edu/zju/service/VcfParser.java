package cn.edu.zju.service;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class VcfParser {

    public static void validateVcf(File file) throws Exception {
        boolean hasHeader = false;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (line.startsWith("##")) {
                    continue;
                }

                if (line.startsWith("#CHROM")) {
                    hasHeader = true;
                    String[] header = line.split("\\t", -1);
                    if (header.length < 5 ||
                            !"#CHROM".equals(header[0]) ||
                            !"POS".equals(header[1]) ||
                            !"ID".equals(header[2]) ||
                            !"REF".equals(header[3]) ||
                            !"ALT".equals(header[4])) {
                        throw new IllegalArgumentException("Invalid VCF header");
                    }
                    continue;
                }

                if (line.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split("\\t", -1);
                if (fields.length < 5) {
                    throw new IllegalArgumentException("Invalid VCF line: fewer than 5 columns");
                }

                if (!fields[1].matches("\\d+")) {
                    throw new IllegalArgumentException("Invalid VCF line: POS is not numeric");
                }

                if (fields[3].isBlank() || fields[4].isBlank()) {
                    throw new IllegalArgumentException("Invalid VCF line: REF or ALT is blank");
                }
            }
        }

        if (!hasHeader) {
            throw new IllegalArgumentException("Invalid VCF file: missing #CHROM header");
        }
    }
}