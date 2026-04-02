package cn.edu.zju.dao;

import cn.edu.zju.bean.Phenotype;
import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public class PhenotypeDao extends BaseDao {

    private static final Logger log = LoggerFactory.getLogger(PhenotypeDao.class);

    /**
     * 根据基因名和 diplotype 查找 phenotype。
     * diplotype 需已规范化（数字小的 allele 在前，如 *1/*2 而非 *2/*1）。
     */
    public Phenotype findByGeneDiplotype(String geneSymbol, String diplotype) {
        String sql = "SELECT id, gene_symbol, diplotype, phenotype, activity_score, function_category " +
                "FROM genotype2phenotype WHERE gene_symbol = ? AND diplotype = ?";
        AtomicReference<Phenotype> ref = new AtomicReference<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setString(1, geneSymbol);
                ps.setString(2, diplotype);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ref.set(new Phenotype(
                            rs.getInt("id"),
                            rs.getString("gene_symbol"),
                            rs.getString("diplotype"),
                            rs.getString("phenotype"),
                            rs.getString("activity_score"),
                            rs.getString("function_category")
                    ));
                }
            } catch (SQLException e) {
                log.error("findByGeneDiplotype error", e);
            }
        });
        return ref.get();
    }
}
