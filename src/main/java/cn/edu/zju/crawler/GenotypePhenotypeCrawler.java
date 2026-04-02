package cn.edu.zju.crawler;

import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;

/**
 * 将 genotype_to_phenotype.csv 导入 genotype2phenotype 表。
 * 一次性执行，重复运行前请先清空目标表：TRUNCATE TABLE genotype2phenotype;
 */
public class GenotypePhenotypeCrawler extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(GenotypePhenotypeCrawler.class);
    private static final String CSV = "genotype_to_phenotype.csv";
    private static final String SQL =
            "INSERT INTO genotype2phenotype (gene_symbol, diplotype, phenotype, " +
            "activity_score, function_category) VALUES (?, ?, ?, ?, ?)";

    public void doImport() {
        log.info("Starting import of {}", CSV);
        int[] count = {0};
        DBUtils.execSQL(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps = connection.prepareStatement(SQL);
                InputStream is = getClass().getClassLoader().getResourceAsStream(CSV);
                if (is == null) {
                    log.error("Resource not found: {}", CSV);
                    return;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    String[] cols = line.split(",", -1);
                    if (cols.length < 5) continue;
                    ps.setString(1, cols[0].trim());  // gene_symbol
                    ps.setString(2, cols[1].trim());  // diplotype
                    ps.setString(3, cols[2].trim());  // phenotype
                    ps.setString(4, cols[3].trim());  // activity_score
                    ps.setString(5, cols[4].trim());  // function_category
                    ps.addBatch();
                    count[0]++;
                    if (count[0] % 1000 == 0) {
                        ps.executeBatch();
                        connection.commit();
                        log.info("Inserted {} rows", count[0]);
                    }
                }
                ps.executeBatch();
                connection.commit();
                reader.close();
                log.info("Import complete: {} rows inserted into genotype2phenotype", count[0]);
            } catch (Exception e) {
                log.error("Import failed", e);
            }
        });
    }
}
