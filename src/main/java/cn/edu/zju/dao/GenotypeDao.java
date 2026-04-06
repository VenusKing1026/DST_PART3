package cn.edu.zju.dao;

import cn.edu.zju.bean.Genotype;
import cn.edu.zju.bean.ScoredAllele;
import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GenotypeDao extends BaseDao {

    private static final Logger log = LoggerFactory.getLogger(GenotypeDao.class);

    /**
     * 根据 rsID 列表查找对应的 star allele。
     * V1 简化：一个 rsID 对应多个 star allele 时取 id 最小的第一条。
     * 使用 GROUP BY rsid 保证每个 rsid 只返回一条记录。
     */
    public List<Genotype> findFirstByRsIds(List<String> rsids) {
        if (rsids == null || rsids.isEmpty()) {
            return new ArrayList<>();
        }
        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < rsids.size(); i++) {
            placeholders.add("?");
        }
        // 子查询取每个 rsid 对应 id 最小的行，兼容 MySQL ONLY_FULL_GROUP_BY 模式
        String sql = "SELECT id, gene_symbol, rsid, chromosome, position, ref_allele, alt_allele, " +
                "star_allele, allele_function, is_required " +
                "FROM variants2genotype " +
                "WHERE id IN (" +
                "  SELECT MIN(id) FROM variants2genotype WHERE rsid IN (" + placeholders + ") GROUP BY rsid" +
                ") " +
                "ORDER BY id ASC";
        List<Genotype> result = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                for (int i = 0; i < rsids.size(); i++) {
                    ps.setString(i + 1, rsids.get(i));
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Genotype g = new Genotype(
                            rs.getInt("id"),
                            rs.getString("gene_symbol"),
                            rs.getString("rsid"),
                            rs.getString("chromosome"),
                            rs.getLong("position"),
                            rs.getString("ref_allele"),
                            rs.getString("alt_allele"),
                            rs.getString("star_allele"),
                            rs.getString("allele_function"),
                            rs.getBoolean("is_required")
                    );
                    result.add(g);
                }
            } catch (SQLException e) {
                log.error("findFirstByRsIds error", e);
            }
        });
        return result;
    }

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
}
