package cn.edu.zju.dao;

import cn.edu.zju.bean.DosingGuideline;
import cn.edu.zju.bean.MatchingResult;
import cn.edu.zju.dbutils.DBUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MatchingResultDao extends BaseDao {

    public void deleteBySampleId(int sampleId) {
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "delete from matching_result where sample_id = ?");
                ps.setInt(1, sampleId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void save(MatchingResult result, String matchType, DosingGuideline guideline) {
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "insert into matching_result(sample_id, gene, diplotype, phenotype, match_type, drug_name, source, summary_markdown) values (?,?,?,?,?,?,?,?)");
                ps.setInt(1, result.getSampleId());
                ps.setString(2, result.getGene());
                ps.setString(3, result.getDiplotype());
                ps.setString(4, result.getPhenotype());
                ps.setString(5, matchType);
                ps.setString(6, guideline.getName());
                ps.setString(7, guideline.getSource());
                ps.setString(8, guideline.getSummaryMarkdown());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveAll(int sampleId, List<MatchingResult> results) {
        deleteBySampleId(sampleId);
        for (MatchingResult result : results) {
            result.setSampleId(sampleId);
            if (result.getMetabolizerMatches() != null) {
                for (DosingGuideline g : result.getMetabolizerMatches()) {
                    save(result, "metabolizer", g);
                }
            }
            if (result.getGeneralMatches() != null) {
                for (DosingGuideline g : result.getGeneralMatches()) {
                    save(result, "general", g);
                }
            }
        }
    }

    public List<MatchingResult> findBySampleId(int sampleId) {
        List<MatchingResult> results = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "select id, sample_id, gene, diplotype, phenotype, match_type, drug_name, source, summary_markdown from matching_result where sample_id = ? order by gene, match_type, drug_name");
                ps.setInt(1, sampleId);
                ResultSet rs = ps.executeQuery();
                String currentGene = null;
                MatchingResult current = null;
                while (rs.next()) {
                    String gene = rs.getString("gene");
                    if (!gene.equals(currentGene)) {
                        currentGene = gene;
                        current = new MatchingResult();
                        current.setId(rs.getInt("id"));
                        current.setSampleId(rs.getInt("sample_id"));
                        current.setGene(gene);
                        current.setDiplotype(rs.getString("diplotype"));
                        current.setPhenotype(rs.getString("phenotype"));
                        current.setMetabolizerMatches(new ArrayList<>());
                        current.setGeneralMatches(new ArrayList<>());
                        results.add(current);
                    }
                    DosingGuideline g = new DosingGuideline();
                    g.setName(rs.getString("drug_name"));
                    g.setSource(rs.getString("source"));
                    g.setSummaryMarkdown(rs.getString("summary_markdown"));
                    String matchType = rs.getString("match_type");
                    if ("metabolizer".equals(matchType)) {
                        current.getMetabolizerMatches().add(g);
                    } else {
                        current.getGeneralMatches().add(g);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return results;
    }
}
