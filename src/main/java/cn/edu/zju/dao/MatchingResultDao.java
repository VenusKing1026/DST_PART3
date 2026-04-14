package cn.edu.zju.dao;

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
                PreparedStatement preparedStatement =
                        connection.prepareStatement("delete from matching_result where sample_id = ?");
                preparedStatement.setInt(1, sampleId);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void save(MatchingResult matchingResult) {
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "insert into matching_result(sample_id, drug_label_id, drug_name, source, summary_markdown) values (?,?,?,?,?)"
                );
                preparedStatement.setInt(1, matchingResult.getSampleId());
                preparedStatement.setString(2, matchingResult.getDrugLabelId());
                preparedStatement.setString(3, matchingResult.getDrugName());
                preparedStatement.setString(4, matchingResult.getSource());
                preparedStatement.setString(5, matchingResult.getSummaryMarkdown());
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveAll(int sampleId, List<MatchingResult> results) {
        deleteBySampleId(sampleId);
        for (MatchingResult result : results) {
            save(result);
        }
    }

    public List<MatchingResult> findBySampleId(int sampleId) {
        List<MatchingResult> results = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "select id, sample_id, drug_label_id, drug_name, source, summary_markdown from matching_result where sample_id = ?"
                );
                preparedStatement.setInt(1, sampleId);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    Integer id = resultSet.getInt("id");
                    Integer resultSampleId = resultSet.getInt("sample_id");
                    String drugLabelId = resultSet.getString("drug_label_id");
                    String drugName = resultSet.getString("drug_name");
                    String source = resultSet.getString("source");
                    String summaryMarkdown = resultSet.getString("summary_markdown");

                    MatchingResult matchingResult = new MatchingResult(
                            id,
                            resultSampleId,
                            drugLabelId,
                            drugName,
                            source,
                            summaryMarkdown
                    );
                    results.add(matchingResult);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return results;
    }
}