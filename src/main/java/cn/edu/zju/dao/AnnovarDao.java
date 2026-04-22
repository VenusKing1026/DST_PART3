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

        String[] headers = lines[0].split("\\t", -1);
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i], i);
        }

        List<String> requiredFields = Arrays.asList("Chr", "Start", "End", "Ref", "Alt");
        for (String field : requiredFields) {
            if (!headerIndex.containsKey(field)) {
                throw new RuntimeException("Invalid ANNOVAR output: missing required column " + field);
            }
        }

        DBUtils.execSQL(connection -> {

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

                List<String> dbFields = Arrays.asList(
                        "Chr", "Start", "End", "Ref", "Alt",
                        "Func.refGene", "Gene.refGene", "GeneDetail.refGene", "ExonicFunc.refGene", "AAChange.refGene",
                        "cytoBand",
                        "1000g2015aug_all", "1000g2015aug_afr", "1000g2015aug_amr", "1000g2015aug_eas", "1000g2015aug_eur", "1000g2015aug_sas",
                        "CLNALLELEID", "CLNDN", "CLNDISDB", "CLNREVSTAT", "CLNSIG"
                );

                Set<String> knownFields = new HashSet<>(dbFields);
                int validRowCount = 0;

                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i];
                    if (line == null || line.isBlank()) {
                        continue;
                    }

                    if (line.startsWith("##") || line.startsWith("#CHROM")) {
                        throw new RuntimeException("Invalid ANNOVAR output: raw VCF header line found at line " + (i + 1));
                    }

                    String[] split = line.split("\\t", -1);

                    if (!isValidAnnovarRow(split, headerIndex)) {
                        throw new RuntimeException("Invalid ANNOVAR data row at line " + (i + 1) + ": " + line);
                    }

                    preparedStatement.setInt(1, sampleId);

                    for (int j = 0; j < dbFields.size(); j++) {
                        String fieldName = dbFields.get(j);
                        preparedStatement.setString(j + 2, normalizeValue(getValue(split, headerIndex, fieldName)));
                    }

                    StringJoiner otherInfo = new StringJoiner("\t");
                    for (int h = 0; h < headers.length; h++) {
                        String header = headers[h];
                        if (!knownFields.contains(header)) {
                            String value = h < split.length ? split[h] : "";
                            otherInfo.add(header + "=" + normalizeValue(value));
                        }
                    }

                    preparedStatement.setString(dbFields.size() + 2, otherInfo.toString());
                    preparedStatement.addBatch();
                    validRowCount++;

                    if (validRowCount % 500 == 0) {
                        preparedStatement.executeBatch();
                        connection.commit();
                    }
                }

                if (validRowCount == 0) {
                    throw new RuntimeException("No valid ANNOVAR rows found");
                }

                preparedStatement.executeBatch();
                connection.commit();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean isValidAnnovarRow(String[] split, Map<String, Integer> headerIndex) {
        String chr = normalizeValue(getValue(split, headerIndex, "Chr"));
        String start = normalizeValue(getValue(split, headerIndex, "Start"));
        String end = normalizeValue(getValue(split, headerIndex, "End"));
        String ref = normalizeValue(getValue(split, headerIndex, "Ref"));
        String alt = normalizeValue(getValue(split, headerIndex, "Alt"));

        return !chr.isBlank()
                && start.matches("\\d+")
                && end.matches("\\d+")
                && !ref.isBlank()
                && !alt.isBlank();
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