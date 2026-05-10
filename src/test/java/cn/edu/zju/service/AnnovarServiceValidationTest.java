package cn.edu.zju.service;

import cn.edu.zju.AppConfig;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AnnovarServiceValidationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final AppConfig config = AppConfig.getInstance();
    private final String originalTableAnnovar = config.getAnnovarTableAnnovar();
    private final String originalHumanDb = config.getAnnovarHumanDb();

    @After
    public void restoreConfig() {
        config.setAnnovarTableAnnovar(originalTableAnnovar);
        config.setAnnovarHumanDb(originalHumanDb);
    }

    @Test
    public void rejectVcfWithoutChromHeader() throws Exception {
        IOException error = expectInvalidVcf("1\t12345\t.\tA\tG\t.\tPASS\t.\n");

        assertTrue(error.getMessage().contains("Missing #CHROM header"));
    }

    @Test
    public void rejectVcfWithoutVariantRecords() throws Exception {
        IOException error = expectInvalidVcf("##fileformat=VCFv4.2\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n");

        assertTrue(error.getMessage().contains("No variant records found"));
    }

    @Test
    public void rejectVcfRecordWithTooFewColumns() throws Exception {
        IOException error = expectInvalidVcf("##fileformat=VCFv4.2\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n1\t12345\t.\tA\n");

        assertTrue(error.getMessage().contains("Expected at least 8 tab-separated columns"));
    }

    private IOException expectInvalidVcf(String content) throws Exception {
        Path inputVcf = temporaryFolder.newFile("invalid.vcf").toPath();
        Files.writeString(inputVcf, content, StandardCharsets.UTF_8);

        config.setAnnovarTableAnnovar("fake-table-annovar");
        config.setAnnovarHumanDb("fake-human-db");

        try {
            new AnnovarService().annotateVcf(inputVcf, 200);
            fail("Expected invalid VCF to be rejected.");
            return null;
        } catch (IOException e) {
            return e;
        }
    }
}
