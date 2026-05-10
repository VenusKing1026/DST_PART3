package cn.edu.zju.service;

import cn.edu.zju.AppConfig;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class AnnovarServiceIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final AppConfig config = AppConfig.getInstance();
    private final String originalPerl = config.getAnnovarPerl();
    private final String originalTableAnnovar = config.getAnnovarTableAnnovar();
    private final String originalHumanDb = config.getAnnovarHumanDb();
    private final String originalBuildver = config.getAnnovarBuildver();
    private final String originalWorkdir = config.getAnnovarWorkdir();

    @After
    public void restoreConfig() {
        config.setAnnovarPerl(originalPerl);
        config.setAnnovarTableAnnovar(originalTableAnnovar);
        config.setAnnovarHumanDb(originalHumanDb);
        config.setAnnovarBuildver(originalBuildver);
        config.setAnnovarWorkdir(originalWorkdir);
    }

    @Test
    public void annotateVcfRunsAnnovarAndReturnsMultiannoContent() throws Exception {
        Path root = temporaryFolder.getRoot().toPath();
        Path annovarDir = root.resolve("annovar");
        Path humanDb = root.resolve("humandb");
        Path workdir = root.resolve("workdir");
        Files.createDirectories(annovarDir);
        Files.createDirectories(humanDb);
        Files.createDirectories(workdir);

        Path fakeTableAnnovar = annovarDir.resolve("table_annovar.java");
        Files.writeString(fakeTableAnnovar, fakeTableAnnovarScript(), StandardCharsets.UTF_8);

        Path inputVcf = root.resolve("input.vcf");
        Files.writeString(inputVcf,
                "##fileformat=VCFv4.2\n"
                        + "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n"
                        + "1\t12345\t.\tA\tG\t.\tPASS\t.\n",
                StandardCharsets.UTF_8);

        config.setAnnovarPerl(Paths.get(System.getProperty("java.home"), "bin", "java.exe").toString());
        config.setAnnovarTableAnnovar(fakeTableAnnovar.toString());
        config.setAnnovarHumanDb(humanDb.toString());
        config.setAnnovarBuildver("hg19");
        config.setAnnovarWorkdir(workdir.toString());

        String result = new AnnovarService().annotateVcf(inputVcf, 101);

        assertTrue(result.contains("Chr\tStart\tEnd\tRef\tAlt\tFunc.refGene\tGene.refGene\tExonicFunc.refGene"));
        assertTrue(result.contains("1\t12345\t12345\tA\tG\texonic\tTPMT\tnonsynonymous SNV"));
        assertTrue(Files.exists(workdir.resolve("sample-101").resolve("annovar.hg19_multianno.txt")));
    }

    private String fakeTableAnnovarScript() {
        return ""
                + "import java.nio.charset.StandardCharsets;\n"
                + "import java.nio.file.Files;\n"
                + "import java.nio.file.Path;\n"
                + "public class table_annovar {\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        int outIndex = indexOf(args, \"-out\");\n"
                + "        int buildIndex = indexOf(args, \"-buildver\");\n"
                + "        if (outIndex == -1 || buildIndex == -1) System.exit(2);\n"
                + "        String outputPrefix = args[outIndex + 1];\n"
                + "        String buildver = args[buildIndex + 1];\n"
                + "        Path outputPath = Path.of(outputPrefix + \".\" + buildver + \"_multianno.txt\");\n"
                + "        Files.createDirectories(outputPath.getParent());\n"
                + "        Files.writeString(outputPath,\n"
                + "                \"Chr\\tStart\\tEnd\\tRef\\tAlt\\tFunc.refGene\\tGene.refGene\\tExonicFunc.refGene\\n\"\n"
                + "                        + \"1\\t12345\\t12345\\tA\\tG\\texonic\\tTPMT\\tnonsynonymous SNV\\n\",\n"
                + "                StandardCharsets.UTF_8);\n"
                + "    }\n"
                + "    private static int indexOf(String[] args, String value) {\n"
                + "        for (int i = 0; i < args.length; i++) {\n"
                + "            if (value.equals(args[i])) return i;\n"
                + "        }\n"
                + "        return -1;\n"
                + "    }\n"
                + "}\n";
    }
}
