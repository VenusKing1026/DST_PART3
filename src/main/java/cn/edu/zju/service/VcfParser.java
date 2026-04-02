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
                if (line.startsWith("##")) {
                    continue;
                }
                if (line.startsWith("#CHROM")) {
                    hasHeader = true;
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split("\\t");
                if (fields.length < 5) {
                    throw new IllegalArgumentException("Invalid VCF line: fewer than 5 columns");
                }
            }
        }

        if (!hasHeader) {
            throw new IllegalArgumentException("Invalid VCF file: missing #CHROM header");
        }
    }
}