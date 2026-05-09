package cn.edu.zju.dao;

import cn.edu.zju.bean.VariantWithGT;
import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnovarDao extends BaseDao {

    private static final Logger log = LoggerFactory.getLogger(AnnovarDao.class.getSimpleName());

    private static final String INSERT_SQL = "INSERT INTO annovar " +
            "(sample_id, Chr, Start, End, Ref, Alt, " +
            "Func_refGene, Gene_refGene, GeneDetail_refGene, ExonicFunc_refGene, " +
            "AAChange_refGene, rsID, GT) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * 从已解析的文本内容入库（annovar 文本上传模式）
     */
    public void save(int sampleId, String content) {
        String[] lines = content.split("\\r|\\n");
        DBUtils.execSQL(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL);
                int batchCount = 0;
                for (String line : lines) {
                    if (line == null || line.isBlank() || line.startsWith("Chr\t") || line.startsWith("#"))
                        continue;
                    String[] split = line.split("\\t", -1);
                    setRefGeneParams(ps, sampleId, split);
                    ps.setString(12, parseRsid(split));
                    ps.setString(13, parseGT(split));
                    ps.addBatch();
                    batchCount++;
                    if (batchCount % 1000 == 0) {
                        ps.executeBatch();
                        connection.commit();
                    }
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                log.error("save(String) failed", e);
            }
        });
    }

    /**
     * 流式读取 multianno 文件并逐行入库，避免大文件 OOM。
     * 只插入功能性变异（exonic + 非同义），在导入时提取 rsID 和 GT。
     */
    public void save(int sampleId, Path filePath) {
        DBUtils.execSQL(connection -> {
            try {
                connection.setAutoCommit(false);
                PreparedStatement ps = connection.prepareStatement(INSERT_SQL);
                int lineNum = 0;
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        if (line.isBlank() || line.startsWith("Chr\t") || line.startsWith("#"))
                            continue;
                        String[] split = line.split("\\t", -1);
                        // 只插入功能性变异：exonic 且非同义突变
                        String funcRefGene = split.length > 5 ? split[5] : ".";
                        String exonicFunc = split.length > 8 ? split[8] : ".";
                        if (!"exonic".equals(funcRefGene) || "synonymous SNV".equals(exonicFunc))
                            continue;
                        setRefGeneParams(ps, sampleId, split);
                        ps.setString(12, parseRsid(split));
                        ps.setString(13, parseGT(split));
                        ps.addBatch();
                        if (lineNum % 1000 == 0) {
                            ps.executeBatch();
                            connection.commit();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read ANNOVAR file: " + filePath, e);
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                log.error("save(Path) failed", e);
            }
        });
    }

    /**
     * 设置 refGene 前 11 个参数（sample_id + 10 列）
     */
    private void setRefGeneParams(PreparedStatement ps, int sampleId, String[] split) throws SQLException {
        ps.setInt(1, sampleId);
        ps.setString(2, split.length > 0 ? split[0] : ".");
        ps.setString(3, split.length > 1 ? split[1] : ".");
        ps.setString(4, split.length > 2 ? split[2] : ".");
        ps.setString(5, split.length > 3 ? split[3] : ".");
        ps.setString(6, split.length > 4 ? split[4] : ".");
        ps.setString(7, split.length > 5 ? split[5] : ".");
        ps.setString(8, split.length > 6 ? split[6] : ".");
        ps.setString(9, split.length > 7 ? split[7] : ".");
        ps.setString(10, split.length > 8 ? split[8] : ".");
        ps.setString(11, split.length > 9 ? split[9] : ".");
    }

    /**
     * 从 tab 分割后的行中解析 rsID。
     * 在 `-otherinfo` 部分中找到 GT FORMAT 字段（如 "GT:AD:DP:..."），
     * 然后向前回溯 6 列获取 VCF ID 字段（VCF 结构：CHROM POS ID REF ALT QUAL FILTER INFO FORMAT）。
     */
    static String parseRsid(String[] split) {
        for (int i = 10; i < split.length - 1; i++) {
            if (split[i].startsWith("GT:") || "GT".equals(split[i])) {
                if (i >= 6) {
                    String candidate = split[i - 6];
                    if (candidate != null && !candidate.isEmpty()) {
                        // VCF ID 可能是 "." 或 "rs12345"
                        return candidate;
                    }
                }
                break;
            }
        }
        return ".";
    }

    /**
     * 从 tab 分割后的行中解析 GT 值。
     * 在 `-otherinfo` 部分中找到 GT FORMAT 字段（如 "GT:AD:DP:..."），
     * 下一个字段即 sample 数据，取其第一个冒号前的部分作为 GT。
     */
    static String parseGT(String[] split) {
        for (int i = 10; i < split.length - 1; i++) {
            if (split[i].startsWith("GT:") || "GT".equals(split[i])) {
                String sample = split[i + 1];
                if (sample != null && !sample.isEmpty()) {
                    int colon = sample.indexOf(':');
                    return colon > 0 ? sample.substring(0, colon) : sample;
                }
                break;
            }
        }
        return null;
    }

    /**
     * 返回样本中功能性变异，从 rsID 和 GT 列直接读取
     */
    public Map<String, List<VariantWithGT>> getVariantsWithGT(int sampleId) {
        String sql = "SELECT Gene_refGene, rsID, Chr, Start, End, Ref, Alt, GT FROM annovar " +
                "WHERE sample_id = ? " +
                "AND Func_refGene = 'exonic' " +
                "AND ExonicFunc_refGene != 'synonymous SNV' " +
                "AND rsID IS NOT NULL AND rsID != '.' " +
                "AND GT IS NOT NULL";

        Map<String, List<VariantWithGT>> geneVariants = new HashMap<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, sampleId);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String gene = rs.getString(1);
                    String rsid = rs.getString(2);

                    if (gene == null || gene.isBlank())
                        continue;
                    if (gene.contains(";"))
                        continue;

                    String chr = rs.getString(3);
                    int start = rs.getInt(4);
                    int end = rs.getInt(5);
                    String ref = rs.getString(6);
                    String alt = rs.getString(7);
                    String gt = rs.getString(8);

                    VariantWithGT variant = new VariantWithGT(gene, rsid, gt);
                    variant.setChr(chr);
                    variant.setStart(start);
                    variant.setEnd(end);
                    variant.setRef(ref);
                    variant.setAlt(alt);

                    geneVariants.computeIfAbsent(gene, k -> new ArrayList<>()).add(variant);
                }
            } catch (SQLException e) {
                log.error("getVariantsWithGT error", e);
            }
        });
        return geneVariants;
    }

    /**
     * 返回样本中功能性变异的 rsID，按基因分组。
     */
    public Map<String, List<String>> getRsIdsPerGene(int sampleId) {
        String sql = "SELECT Gene_refGene, rsID FROM annovar " +
                "WHERE sample_id = ? " +
                "AND Func_refGene = 'exonic' " +
                "AND ExonicFunc_refGene != 'synonymous SNV' " +
                "AND rsID IS NOT NULL AND rsID != '.'";
        Map<String, List<String>> geneRsIds = new HashMap<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, sampleId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String gene = rs.getString(1);
                    String rsid = rs.getString(2);
                    if (gene == null || gene.isBlank())
                        continue;
                    if (gene.contains(";"))
                        continue;
                    geneRsIds.computeIfAbsent(gene, k -> new ArrayList<>()).add(rsid);
                }
            } catch (SQLException e) {
                log.error("getRsIdsPerGene error", e);
            }
        });
        return geneRsIds;
    }

    public List<String> getRefGenes(int sampleId) {
        String sql = "SELECT DISTINCT Gene_refGene FROM annovar " +
                "WHERE ExonicFunc_refGene != 'synonymous SNV' AND sample_id = ?";
        List<String> genes = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setInt(1, sampleId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    genes.add(rs.getString(1));
                }
            } catch (SQLException e) {
                log.error("getRefGenes error", e);
            }
        });
        return genes;
    }
}
