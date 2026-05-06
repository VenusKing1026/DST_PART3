package cn.edu.zju.service;

import cn.edu.zju.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class AnnovarService {

    private static final Logger log = LoggerFactory.getLogger(AnnovarService.class);

    private final AppConfig config = AppConfig.getInstance();

    public String annotateVcf(Part filePart, int sampleId) throws IOException, InterruptedException {
        Path sampleDir = createSampleWorkDir(sampleId);
        Path inputVcf = sampleDir.resolve("input.vcf");
        Files.copy(filePart.getInputStream(), inputVcf, StandardCopyOption.REPLACE_EXISTING);
        return annotateVcf(inputVcf, sampleId);
    }

    public String annotateVcf(Path inputVcf, int sampleId) throws IOException, InterruptedException {
        validateConfig();

        Path sampleDir = createSampleWorkDir(sampleId);
        validateVcf(inputVcf);

        Path tableAnnovar = Paths.get(config.getAnnovarTableAnnovar());
        Path tableAnnovarDir = tableAnnovar.getParent();
        String tableAnnovarScript = tableAnnovar.getFileName().toString();
        String outputPrefix = sampleDir.resolve("annovar").toString();
        List<String> command = new ArrayList<>();
        command.add(config.getAnnovarPerl());
        command.add(tableAnnovarScript);
        command.add(inputVcf.toString());
        command.add(config.getAnnovarHumanDb());
        command.add("-buildver");
        command.add(config.getAnnovarBuildver());
        command.add("-out");
        command.add(outputPrefix);
        command.add("-remove");
        command.add("-protocol");
        command.add("refGene");
        command.add("-operation");
        command.add("g");
        command.add("-nastring");
        command.add(".");
        command.add("-polish");
        command.add("-vcfinput");

        log.info("Running ANNOVAR command: {}", command);
        Process process = new ProcessBuilder(command)
                .directory(tableAnnovarDir == null ? sampleDir.toFile() : tableAnnovarDir.toFile())
                .redirectErrorStream(true)
                .start();

        String processOutput = readProcessOutput(process);
        int exitCode = process.waitFor();

        Path multianno = sampleDir.resolve("annovar." + config.getAnnovarBuildver() + "_multianno.txt");
        if (!Files.exists(multianno)) {
            throw new IOException("ANNOVAR failed with exit code " + exitCode + ": " + processOutput);
        }
        if (exitCode != 0) {
            log.warn("ANNOVAR exited with code {}, but result file was created. Output: {}", exitCode, processOutput);
        }

        return Files.readString(multianno, StandardCharsets.UTF_8);
    }

    public Path createSampleWorkDir(int sampleId) throws IOException {
        Path sampleDir = Paths.get(config.getAnnovarWorkdir(), "sample-" + sampleId);
        Files.createDirectories(sampleDir);
        return sampleDir;
    }

    private void validateConfig() {
        if (isBlank(config.getAnnovarTableAnnovar())) {
            throw new IllegalStateException("ANNOVAR is not configured. Please set annovar.table_annovar in app.properties.");
        }
        if (isBlank(config.getAnnovarHumanDb())) {
            throw new IllegalStateException("ANNOVAR database is not configured. Please set annovar.humandb in app.properties.");
        }
    }

    private void validateVcf(Path inputVcf) throws IOException {
        boolean hasHeader = false;
        boolean hasVariant = false;

        try (BufferedReader reader = Files.newBufferedReader(inputVcf, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#CHROM")) {
                    hasHeader = true;
                    continue;
                }
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t");
                if (fields.length < 8) {
                    throw new IOException("Invalid VCF record. Expected at least 8 tab-separated columns: " + line);
                }
                hasVariant = true;
            }
        }

        if (!hasHeader) {
            throw new IOException("Invalid VCF file. Missing #CHROM header line.");
        }
        if (!hasVariant) {
            throw new IOException("Invalid VCF file. No variant records found.");
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
