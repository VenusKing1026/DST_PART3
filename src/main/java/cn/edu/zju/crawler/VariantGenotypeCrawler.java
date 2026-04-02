package cn.edu.zju.crawler;

import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * 将 variant_to_genotype.csv 导入 variants2genotype 表。
 * 一次性执行，重复运行前请先清空目标表：TRUNCATE TABLE variants2genotype;
 */
public class VariantGenotypeCrawler extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(VariantGenotypeCrawler.class);
    private static final String CSV = "variant_to_genotype.csv";
    private static final String SQL =
            "INSERT INTO variants2genotype (gene_symbol, rsid, chromosome, position, " +
            "ref_allele, alt_allele, star_allele, allele_function, is_required) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                    if (cols.length < 9) continue;
                    ps.setString(1, cols[0].trim());  // gene_symbol
                    ps.setString(2, cols[1].trim());  // rsid
                    ps.setString(3, cols[2].trim());  // chromosome
                    String pos = cols[3].trim();
                    ps.setObject(4, pos.isEmpty() ? null : Long.parseLong(pos));  // position
                    ps.setString(5, cols[4].trim());  // ref_allele
                    ps.setString(6, cols[5].trim());  // alt_allele
                    ps.setString(7, cols[6].trim());  // star_allele
                    ps.setString(8, cols[7].trim());  // allele_function
                    String req = cols[8].trim();
                    ps.setObject(9, req.isEmpty() ? null : "TRUE".equalsIgnoreCase(req) ? 1 : 0);
                    ps.addBatch();
                    count[0]++;
                    if (count[0] % 500 == 0) {
                        ps.executeBatch();
                        connection.commit();
                        log.info("Inserted {} rows", count[0]);
                    }
                }
                ps.executeBatch();
                connection.commit();
                reader.close();
                log.info("Import complete: {} rows inserted into variants2genotype", count[0]);
            } catch (Exception e) {
                log.error("Import failed", e);
            }
        });
    }
}
