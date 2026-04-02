package cn.edu.zju.dao;

import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AnnovarDao extends BaseDao {

    private Logger log = LoggerFactory.getLogger(AnnovarDao.class.getSimpleName());

    public void save(int sampleId, String content) {
        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) {
            throw new RuntimeException("ANNOVAR output is empty or missing data rows");
        }

        DBUtils.execSQL(connection -> {

            // 只插入当前 AnnovarService 实际会产生的字段
            String sql = "INSERT INTO annovar (" +
                    "sample_id, " +
                    "Chr, Start, End, Ref, Alt, " +
                    "`Func.refGene`, `Gene.refGene`, `GeneDetail.refGene`, `ExonicFunc.refGene`, `AAChange.refGene`, " +
                    "cytoBand, " +
                    "`1000g2015aug_all`, `1000g2015aug_afr`, `1000g2015aug_amr`, `1000g2015aug_eas`, `1000g2015aug_eur`, `1000g2015aug_sas`, " +
                    "CLNALLELEID, CLNDN, CLNDISDB, CLNREVSTAT, CLNSIG, " +
                    "Otherinfo" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";

            try {
                connection.setAutoCommit(false);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);

                // 读取表头
                String[] headers = lines[0].split("\\t", -1);
                Map<String, Integer> headerIndex = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    headerIndex.put(headers[i], i);
                }

                // 当前版本 runAnnovar() 实际会返回的固定字段
                List<String> dbFields = Arrays.asList(
                        "Chr", "Start", "End", "Ref", "Alt",
                        "Func.refGene", "Gene.refGene", "GeneDetail.refGene", "ExonicFunc.refGene", "AAChange.refGene",
                        "cytoBand",
                        "1000g2015aug_all", "1000g2015aug_afr", "1000g2015aug_amr", "1000g2015aug_eas", "1000g2015aug_eur", "1000g2015aug_sas",
                        "CLNALLELEID", "CLNDN", "CLNDISDB", "CLNREVSTAT", "CLNSIG"
                );

                Set<String> knownFields = new HashSet<>(dbFields);

                for (int i = 1; i < lines.length; i++) {
                    if (lines[i] == null || lines[i].isBlank()) {
                        continue;
                    }

                    // 跳过 VCF 头信息混入的行
                    if (lines[i].startsWith("##") || lines[i].startsWith("#CHROM")) {
                        continue;
                    }

                    String[] split = lines[i].split("\\t", -1);

                    // 如果连最基本的变异列都没有，跳过
                    if (split.length < 5) {
                        continue;
                    }

                    // 第1列：sample_id
                    preparedStatement.setInt(1, sampleId);

                    // 第2列开始：固定字段
                    for (int j = 0; j < dbFields.size(); j++) {
                        String fieldName = dbFields.get(j);
                        preparedStatement.setString(j + 2, normalizeValue(getValue(split, headerIndex, fieldName)));
                    }

                    // 其余所有非固定字段，统一拼到 Otherinfo
                    StringJoiner otherInfo = new StringJoiner("\t");
                    for (int h = 0; h < headers.length; h++) {
                        String header = headers[h];
                        if (!knownFields.contains(header)) {
                            String value = h < split.length ? split[h] : "";
                            otherInfo.add(header + "=" + normalizeValue(value));
                        }
                    }

                    // 最后一列：Otherinfo
                    preparedStatement.setString(dbFields.size() + 2, otherInfo.toString());

                    preparedStatement.addBatch();

                    if (i % 500 == 0) {
                        preparedStatement.executeBatch();
                        connection.commit();
                    }
                }

                preparedStatement.executeBatch();
                connection.commit();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getValue(String[] split, Map<String, Integer> headerIndex, String columnName) {
        Integer idx = headerIndex.get(columnName);
        if (idx == null || idx >= split.length) {
            return "";
        }
        return split[idx];
    }

    private String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        value = value.trim();
        return ".".equals(value) ? "" : value;
    }

    public List<String> getRefGenes(int sampleId) {
        String sql = "select distinct `Gene.refGene` from annovar " +
                "where sample_id = ? " +
                "and `Gene.refGene` is not null " +
                "and `Gene.refGene` <> '' " +
                "and (`ExonicFunc.refGene` is null or `ExonicFunc.refGene` <> 'synonymous SNV')";

        List<String> genes = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setInt(1, sampleId);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    genes.add(resultSet.getString(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return genes;
    }
}