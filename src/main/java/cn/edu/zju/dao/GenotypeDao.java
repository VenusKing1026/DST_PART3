package cn.edu.zju.dao;

import cn.edu.zju.bean.Genotype;
import cn.edu.zju.bean.ScoredAllele;
import cn.edu.zju.bean.VariantWithGT;
import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.StringJoiner;

public class GenotypeDao extends BaseDao {

    private static final Logger log = LoggerFactory.getLogger(GenotypeDao.class);

    /**
     * 根据 rsID 列表查找对应的 star allele。
     * V1 简化：一个 rsID 对应多个 star allele 时取 id 最小的第一条。
     * 使用 GROUP BY rsid 保证每个 rsid 只返回一条记录。
     */
    // public List<Genotype> findFirstByRsIds(List<String> rsids) {
    //     if (rsids == null || rsids.isEmpty()) {
    //         return new ArrayList<>();
    //     }
    //     StringJoiner placeholders = new StringJoiner(", ");
    //     for (int i = 0; i < rsids.size(); i++) {
    //         placeholders.add("?");
    //     }
    //     // 子查询取每个 rsid 对应 id 最小的行，兼容 MySQL ONLY_FULL_GROUP_BY 模式
    //     String sql = "SELECT id, gene_symbol, rsid, chromosome, position, ref_allele, alt_allele, " +
    //             "star_allele, allele_function, is_required " +
    //             "FROM variants2genotype " +
    //             "WHERE id IN (" +
    //             "  SELECT MIN(id) FROM variants2genotype WHERE rsid IN (" + placeholders + ") GROUP BY rsid" +
    //             ") " +
    //             "ORDER BY id ASC";
    //     List<Genotype> result = new ArrayList<>();
    //     DBUtils.execSQL(connection -> {
    //         try {
    //             PreparedStatement ps = connection.prepareStatement(sql);
    //             for (int i = 0; i < rsids.size(); i++) {
    //                 ps.setString(i + 1, rsids.get(i));
    //             }
    //             ResultSet rs = ps.executeQuery();
    //             while (rs.next()) {
    //                 Genotype g = new Genotype(
    //                         rs.getInt("id"),
    //                         rs.getString("gene_symbol"),
    //                         rs.getString("rsid"),
    //                         rs.getString("chromosome"),
    //                         rs.getLong("position"),
    //                         rs.getString("ref_allele"),
    //                         rs.getString("alt_allele"),
    //                         rs.getString("star_allele"),
    //                         rs.getString("allele_function"),
    //                         rs.getBoolean("is_required")
    //                 );
    //                 result.add(g);
    //             }
    //         } catch (SQLException e) {
    //             log.error("findFirstByRsIds error", e);
    //         }
    //     });
    //     return result;
    // }

    /**
     * V2 打分策略：对指定基因的每个候选 star allele，统计患者 rsID 集合命中了多少个。
     * 返回列表按 matched_count DESC, total_count ASC 排序（命中多且所需少的排最前）。
     *
     * @param gene        基因名（如 CYP2C19）
     * @param patientRsids 患者该基因下的功能性 rsID 列表
     */
    public List<ScoredAllele> findScoredAlleles(String gene, List<String> patientRsids) {
        if (patientRsids == null || patientRsids.isEmpty()) {
            return new ArrayList<>();
        }
        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < patientRsids.size(); i++) {
            placeholders.add("?");
        }
        String sql = "SELECT v.star_allele, COUNT(*) AS matched_count, t.total_count " +
                "FROM variants2genotype v " +
                "JOIN ( " +
                "  SELECT star_allele, COUNT(*) AS total_count " +
                "  FROM variants2genotype WHERE gene_symbol = ? GROUP BY star_allele " +
                ") t ON v.star_allele = t.star_allele " +
                "WHERE v.gene_symbol = ? AND v.rsid IN (" + placeholders + ") " +
                "GROUP BY v.star_allele, t.total_count " +
                "ORDER BY matched_count DESC, t.total_count ASC";

        List<ScoredAllele> result = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                int idx = 1;
                ps.setString(idx++, gene);   // subquery gene_symbol
                ps.setString(idx++, gene);   // outer gene_symbol
                for (String rsid : patientRsids) {
                    ps.setString(idx++, rsid);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    result.add(new ScoredAllele(
                            rs.getString("star_allele"),
                            rs.getInt("matched_count"),
                            rs.getInt("total_count")
                    ));
                }
            } catch (SQLException e) {
                log.error("findScoredAlleles error", e);
            }
        });
        return result;
    }

    /**
     * V4.1 染色体感知的 diplotype 推断策略（正确版本）：
     * 1. 根据 GT 信息拆分两条染色体各自的 ALT rsID 集合
     * 2. 对每条染色体分别调用 V2 打分策略 findScoredAlleles()
     * 3. 取各自最高分 allele 组合得到 diplotype
     *
     * @param gene 基因名（如 CYP2C19）
     * @param variantsWithGT 患者该基因下的变异及其GT信息列表
     * @return DiplotypeResult 包含两条染色体的最佳 allele 及 diplotype
     */
    public DiplotypeResult inferDiplotypeWithGT(String gene, List<VariantWithGT> variantsWithGT) {
        if (variantsWithGT == null || variantsWithGT.isEmpty()) {
            return new DiplotypeResult("*1", "*1", "*1/*1");
        }

        // Step 1: 拆分两条染色体各自的 ALT rsID 集合
        List<String> chr1Rsids = new ArrayList<>();
        List<String> chr2Rsids = new ArrayList<>();

        for (VariantWithGT variant : variantsWithGT) {
            String gt = variant.getGt();
            if (gt == null || gt.isEmpty()) continue;

            // 标准化分隔符，解析 GT
            String normalizedGT = gt.replace('|', '/');
            String[] alleles = normalizedGT.split("/");

            if (alleles.length < 2) continue;

            try {
                int allele1 = Integer.parseInt(alleles[0]);  // 染色体1
                int allele2 = Integer.parseInt(alleles[1]);  // 染色体2

                // 非0 表示 ALT，加入对应染色体的 rsID 集合
                if (allele1 != 0) chr1Rsids.add(variant.getRsid());
                if (allele2 != 0) chr2Rsids.add(variant.getRsid());
            } catch (NumberFormatException e) {
                log.warn("Cannot parse GT: {} for rsid: {}", gt, variant.getRsid());
            }
        }

        log.debug("[V4.1] gene={}, chr1Rsids={}, chr2Rsids={}", gene, chr1Rsids, chr2Rsids);

        // Step 2: 对每条染色体分别调用 V2 打分策略
        List<ScoredAllele> chr1Scores = findScoredAlleles(gene, chr1Rsids);
        List<ScoredAllele> chr2Scores = findScoredAlleles(gene, chr2Rsids);

        // Step 3: 取各自最高分 allele（空集默认 *1）
        String allele1 = chr1Scores.isEmpty() ? "*1" : chr1Scores.get(0).getStarAllele();
        String allele2 = chr2Scores.isEmpty() ? "*1" : chr2Scores.get(0).getStarAllele();

        // Step 4: 组合并规范化 diplotype
        String diplotype = canonicalize(allele1, allele2);

        log.info("[V4.1] gene={}, allele1={}, allele2={}, diplotype={}", gene, allele1, allele2, diplotype);

        return new DiplotypeResult(allele1, allele2, diplotype);
    }

    /**
     * 规范化 diplotype 格式：小号/大号（如 *1/*2 而非 *2/*1）
     */
    private String canonicalize(String a1, String a2) {
        int n1 = parseStarNumber(a1);
        int n2 = parseStarNumber(a2);
        if (n1 <= n2) return a1 + "/" + a2;
        return a2 + "/" + a1;
    }

    private int parseStarNumber(String allele) {
        try {
            return Integer.parseInt(allele.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * V4.1 Diplotype 推断结果
     */
    public static class DiplotypeResult {
        private final String allele1;       // 染色体1最佳 allele
        private final String allele2;       // 染色体2最佳 allele
        private final String diplotype;     // 组合后的 diplotype

        public DiplotypeResult(String allele1, String allele2, String diplotype) {
            this.allele1 = allele1;
            this.allele2 = allele2;
            this.diplotype = diplotype;
        }

        public String getAllele1() { return allele1; }
        public String getAllele2() { return allele2; }
        public String getDiplotype() { return diplotype; }

        @Override
        public String toString() {
            return String.format("DiplotypeResult{allele1=%s, allele2=%s, diplotype=%s}", allele1, allele2, diplotype);
        }
    }

    // ============================================================================
    // V4.1 错误版本（已废弃）- 之前的加权打分逻辑不正确
    // ============================================================================
    // 问题：将所有 rsID 的 GT 权重加到同一个 allele 上，无法区分两条染色体
    // 正确做法：拆分两条染色体的 rsID 集合，分别打分
    // ============================================================================
    /*
    public List<ScoredAllele> findScoredAllelesWithGT(String gene, List<VariantWithGT> variantsWithGT) {
        if (variantsWithGT == null || variantsWithGT.isEmpty()) {
            return new ArrayList<>();
        }

        // 创建rsID到GT权重的映射
        Map<String, Double> rsidToWeight = new HashMap<>();
        for (VariantWithGT variant : variantsWithGT) {
            double weight = calculateGTWeight(variant.getGt());
            rsidToWeight.put(variant.getRsid(), weight);
        }

        // 获取该基因的所有候选星等位基因及它们所需的rsID
        String sql = "SELECT v.star_allele, v.rsid, t.total_count " +
                "FROM variants2genotype v " +
                "JOIN ( " +
                "  SELECT star_allele, COUNT(*) AS total_count " +
                "  FROM variants2genotype WHERE gene_symbol = ? GROUP BY star_allele " +
                ") t ON v.star_allele = t.star_allele " +
                "WHERE v.gene_symbol = ?";

        // 存储每个star等位基因的加权得分
        Map<String, WeightedScore> scores = new HashMap<>();

        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, gene);
                ps.setString(2, gene);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String starAllele = rs.getString("star_allele");
                    String rsid = rs.getString("rsid");
                    int totalCount = rs.getInt("total_count");

                    // 获取对应rsID的权重
                    double weight = rsidToWeight.getOrDefault(rsid, 0.0);

                    scores.computeIfAbsent(starAllele, k -> new WeightedScore(starAllele, totalCount))
                          .addWeightedScore(weight);
                }
            } catch (SQLException e) {
                log.error("findScoredAllelesWithGT error", e);
            }
        });

        // 转换为ScoredAllele列表并按加权得分排序
        List<ScoredAllele> result = new ArrayList<>();
        scores.values().stream()
              .map(ws -> new ScoredAllele(ws.starAllele, (int)Math.round(ws.weightedScore), ws.totalRequired))
              .sorted((a, b) -> {
                  // 首先按加权匹配数降序排列
                  int matchedDiff = b.getMatchedCount() - a.getMatchedCount();
                  if (matchedDiff != 0) {
                      return matchedDiff;
                  }
                  // 如果匹配数相同，则按所需总数升序排列
                  return a.getTotalRequired() - b.getTotalRequired();
              })
              .forEach(result::add);

        return result;
    }

    private double calculateGTWeight(String gt) {
        if (gt == null || gt.isEmpty()) {
            return 0.0;
        }

        String normalizedGT = gt.replace('|', '/');
        String[] alleles = normalizedGT.split("/");

        if (alleles.length < 2) {
            return 0.0;
        }

        try {
            int allele1 = Integer.parseInt(alleles[0]);
            int allele2 = Integer.parseInt(alleles[1]);

            if (allele1 == 0 && allele2 == 0) {
                return 0.0;
            } else if (allele1 == allele2 && allele1 != 0) {
                return 1.0;
            } else if (allele1 != allele2) {
                return 0.5;
            } else if (allele1 != 0) {
                return 1.0;
            } else {
                return 0.0;
            }
        } catch (NumberFormatException e) {
            log.warn("Cannot parse GT: {}, returning default weight", gt);
            return 0.0;
        }
    }

    private static class WeightedScore {
        final String starAllele;
        final int totalRequired;
        double weightedScore = 0.0;

        WeightedScore(String starAllele, int totalRequired) {
            this.starAllele = starAllele;
            this.totalRequired = totalRequired;
        }

        void addWeightedScore(double weight) {
            this.weightedScore += weight;
        }
    }
    */
}
