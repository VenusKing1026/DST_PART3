package cn.edu.zju.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AnnovarService {

    private static final String PERL_CMD = "perl";
    private static final String ANNOVAR_DIR = "C:\\Users\\15656\\Documents\\annovar.latest.tar\\annovar\\annovar";
    private static final String HUMAN_DB_DIR = "C:\\Users\\15656\\Documents\\annovar.latest.tar\\annovar\\annovar\\humandb";

    public static File runAnnovar(File inputFile, boolean vcfInput) throws Exception {
        if (inputFile == null || !inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile);
        }

        // 如果输入是 VCF，先转成 avinput
        File actualInputFile = inputFile;
        if (vcfInput) {
            actualInputFile = convertVcfToAvinput(inputFile);
        }

        String baseName = removeExtension(actualInputFile.getName()) + "_" + System.currentTimeMillis();

        File outDir = new File(ANNOVAR_DIR, "tmp_output");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new RuntimeException("Failed to create output directory: " + outDir.getAbsolutePath());
        }

        String outPrefix = new File(outDir, baseName).getAbsolutePath();

        List<String> command = new ArrayList<>();
        command.add(PERL_CMD);
        command.add("table_annovar.pl");
        command.add(actualInputFile.getAbsolutePath());
        command.add(HUMAN_DB_DIR);
        command.add("-buildver");
        command.add("hg19");
        command.add("-out");
        command.add(outPrefix);

        // 调试阶段先不要 -remove，方便排查中间文件
        // command.add("-remove");

        command.add("-protocol");
        command.add("refGene,cytoBand,1000g2015aug_all,1000g2015aug_afr,1000g2015aug_amr,1000g2015aug_eas,1000g2015aug_eur,1000g2015aug_sas,clinvar_20180603");
        command.add("-operation");
        command.add("g,r,f,f,f,f,f,f,f");
        command.add("-nastring");
        command.add(".");
        command.add("-polish");
        command.add("-otherinfo");

        // 这里只有直接输入 VCF 时才需要 -vcfinput
        // 现在如果已经转成 avinput，就不要再加了
        if (!vcfInput) {
            // 输入本身就是 avinput，不需要 -vcfinput
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(ANNOVAR_DIR));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder log = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append(System.lineSeparator());
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        File result = new File(outPrefix + ".hg19_multianno.txt");

        // 最关键：先看结果文件在不在
        if (!result.exists()) {
            throw new RuntimeException("ANNOVAR failed: multianno.txt not generated.\n" + log);
        }

        // 即使 exitCode != 0，也不立刻失败；有些情况下结果文件已经生成
        if (exitCode != 0) {
            System.err.println("WARNING: ANNOVAR exited with code " + exitCode
                    + ", but multianno.txt exists. Continue...");
        }

        validateMultiannoFile(result);
        return result;
    }

    /**
     * 把 VCF 转成 avinput
     */
    private static File convertVcfToAvinput(File vcfFile) throws Exception {
        String baseName = removeExtension(vcfFile.getName()) + "_" + System.currentTimeMillis();

        File outDir = new File(ANNOVAR_DIR, "tmp_output");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new RuntimeException("Failed to create temp output directory: " + outDir.getAbsolutePath());
        }

        File avinputFile = new File(outDir, baseName + ".avinput");

        List<String> command = new ArrayList<>();
        command.add(PERL_CMD);
        command.add("convert2annovar.pl");
        command.add("-format");
        command.add("vcf4");
        command.add("-includeinfo");
        command.add(vcfFile.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(ANNOVAR_DIR));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder log = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(avinputFile), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append(System.lineSeparator());

                // convert2annovar 的真正结果写进 avinput 文件
                // 跳过日志型输出（保险一点）
                if (!line.startsWith("NOTICE") && !line.startsWith("WARNING") && !line.startsWith("Error")) {
                    writer.write(line);
                    writer.newLine();
                }

                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();

        if (!avinputFile.exists() || avinputFile.length() == 0) {
            throw new RuntimeException("convert2annovar failed: avinput not generated or empty.\n" + log);
        }

        if (exitCode != 0) {
            System.err.println("WARNING: convert2annovar exited with code " + exitCode
                    + ", but avinput exists. Continue...");
        }

        validateAvinputFile(avinputFile);
        return avinputFile;
    }

    private static String removeExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static void validateAvinputFile(File avinputFile) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(avinputFile), StandardCharsets.UTF_8))) {

            String firstLine = br.readLine();
            if (firstLine == null || firstLine.isBlank()) {
                throw new IllegalArgumentException("Generated avinput file is empty");
            }

            String[] fields = firstLine.split("\\t", -1);
            if (fields.length < 5) {
                throw new IllegalArgumentException("Invalid avinput format: too few columns");
            }

            if (!fields[1].matches("\\d+") || !fields[2].matches("\\d+")) {
                throw new IllegalArgumentException("Invalid avinput format: start/end must be numeric");
            }
        }
    }

    private static void validateMultiannoFile(File result) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(result), StandardCharsets.UTF_8))) {

            String header = br.readLine();
            String secondLine = br.readLine();

            if (header == null || !header.startsWith("Chr\tStart\tEnd\tRef\tAlt")) {
                throw new IllegalArgumentException("Invalid ANNOVAR output header");
            }

            if (secondLine == null || secondLine.isBlank()) {
                throw new IllegalArgumentException("ANNOVAR output has no data rows");
            }

            if (secondLine.startsWith("##") || secondLine.startsWith("#CHROM")) {
                throw new IllegalArgumentException("ANNOVAR output looks like raw VCF, not multianno");
            }

            String[] fields = secondLine.split("\\t", -1);
            if (fields.length < 5) {
                throw new IllegalArgumentException("ANNOVAR output row has too few columns");
            }

            if (!fields[1].matches("\\d+") || !fields[2].matches("\\d+")) {
                throw new IllegalArgumentException("ANNOVAR output row is invalid: Start/End must be numeric");
            }
        }
    }
}