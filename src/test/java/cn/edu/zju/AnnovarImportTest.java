package cn.edu.zju;

import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.SampleDao;
import cn.edu.zju.bean.Sample;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * 手动导入已存在的 ANNOVAR multianno 文件到数据库。
 * 用于 ANNOVAR 跑完后但 Java 处理失败时，不重新跑 ANNOVAR，直接导入已有结果。
 *
 * 用法： java cn.edu.zju.AnnovarImportTest <sampleId> <multiannoFilePath>
 * 示例： java cn.edu.zju.AnnovarImportTest 1023 E:\tmp\dst-annovar\sample-1023\annovar.hg19_multianno.txt
 */
public class AnnovarImportTest {

    public static void main(String[] args) throws Exception {
        int sampleId;
        Path multiannoFile;

        if (args.length >= 2) {
            sampleId = Integer.parseInt(args[0]);
            multiannoFile = Paths.get(args[1]);
        } else {
            // 默认值
            sampleId = 1023;
            multiannoFile = Paths.get("E:/tmp/dst-annovar/sample-1023/annovar.hg19_multianno.txt");
        }

        if (!Files.exists(multiannoFile)) {
            System.err.println("File not found: " + multiannoFile.toAbsolutePath());
            System.exit(1);
        }

        long fileSize = Files.size(multiannoFile);
        System.out.println("=== ANNOVAR Import Test ===");
        System.out.println("Sample ID: " + sampleId);
        System.out.println("File: " + multiannoFile.toAbsolutePath());
        System.out.println("Size: " + fileSize + " bytes (" + (fileSize / 1024 / 1024) + " MB)");

        // 确保 sample 记录存在
        SampleDao sampleDao = new SampleDao();
        Sample existing = sampleDao.findById(sampleId);
        if (existing == null) {
            System.out.println("Sample " + sampleId + " not found, creating a placeholder...");
            Sample sample = new Sample();
            sample.setUserId(1);
            sample.setCreatedAt(new Date());
            sample.setUploadedBy("import");
            sample.setInputType("vcf");
            sample.setFileName(multiannoFile.getFileName().toString());
            sample.setParseStatus("importing");
            int newId = sampleDao.save(sample);
            System.out.println("Created sample with id=" + newId);
            sampleId = newId;
        } else {
            System.out.println("Found existing sample: " + existing.getFileName()
                    + " (status=" + existing.getParseStatus() + ")");
            sampleDao.updateParseStatus(sampleId, "processing");
        }

        // 调用新的流式 save(Path) 方法
        System.out.println("Starting streaming import...");
        long start = System.currentTimeMillis();

        AnnovarDao annovarDao = new AnnovarDao();
        annovarDao.save(sampleId, multiannoFile);

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Import completed in " + (elapsed / 1000) + " seconds");

        // 更新状态
        sampleDao.updateParseStatus(sampleId, "finished");
        System.out.println("Sample status updated to 'finished'");
        System.out.println("=== Done ===");
    }
}
