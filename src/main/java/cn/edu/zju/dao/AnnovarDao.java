package cn.edu.zju.dao;

import cn.edu.zju.bean.VariantWithGT;
import cn.edu.zju.dbutils.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnovarDao extends BaseDao {

    private Logger log = LoggerFactory.getLogger(AnnovarDao.class.getSimpleName());

    public void save(int sampleId, String content) {
        String[] lines = content.split("\\r|\\n");
        DBUtils.execSQL(connection -> {
            String sql = "INSERT INTO annovar (sample_id, Chr, Start, End, Ref, Alt, `Func.refGene`, `Gene.refGene`, `GeneDetail.refGene`, `ExonicFunc.refGene`, `AAChange.refGene`, cytoBand, `1000g2015aug_all`, `1000g2015aug_afr`, `1000g2015aug_amr`, `1000g2015aug_eas`, `1000g2015aug_eur`, `1000g2015aug_sas`, ExAC_ALL, ExAC_AFR, ExAC_AMR, ExAC_EAS, ExAC_FIN, ExAC_NFE, ExAC_OTH, ExAC_SAS, avsnp150, esp6500siv2_all, esp6500siv2_ea, esp6500siv2_aa, gnomAD_exome_ALL, gnomAD_exome_AFR, gnomAD_exome_AMR, gnomAD_exome_ASJ, gnomAD_exome_EAS, gnomAD_exome_FIN, gnomAD_exome_NFE, gnomAD_exome_OTH, gnomAD_exome_SAS, SIFT_score, SIFT_converted_rankscore, SIFT_pred, Polyphen2_HDIV_score, Polyphen2_HDIV_rankscore, Polyphen2_HDIV_pred, Polyphen2_HVAR_score, Polyphen2_HVAR_rankscore, Polyphen2_HVAR_pred, LRT_score, LRT_converted_rankscore, LRT_pred, MutationTaster_score, MutationTaster_converted_rankscore, MutationTaster_pred, MutationAssessor_score, MutationAssessor_score_rankscore, MutationAssessor_pred, FATHMM_score, FATHMM_converted_rankscore, FATHMM_pred, PROVEAN_score, PROVEAN_converted_rankscore, PROVEAN_pred, VEST3_score, VEST3_rankscore, MetaSVM_score, MetaSVM_rankscore, MetaSVM_pred, MetaLR_score, MetaLR_rankscore, MetaLR_pred, `M-CAP_score`, `M-CAP_rankscore`, `M-CAP_pred`, REVEL_score, REVEL_rankscore, MutPred_score, MutPred_rankscore, CADD_raw, CADD_raw_rankscore, CADD_phred, DANN_score, DANN_rankscore, `fathmm-MKL_coding_score`, `fathmm-MKL_coding_rankscore`, `fathmm-MKL_coding_pred`, Eigen_coding_or_noncoding, `Eigen-raw`, `Eigen-PC-raw`, GenoCanyon_score, GenoCanyon_score_rankscore, integrated_fitCons_score, integrated_fitCons_score_rankscore, integrated_confidence_value, `GERP++_RS`, `GERP++_RS_rankscore`, phyloP100way_vertebrate, phyloP100way_vertebrate_rankscore, phyloP20way_mammalian, phyloP20way_mammalian_rankscore, phastCons100way_vertebrate, phastCons100way_vertebrate_rankscore, phastCons20way_mammalian, phastCons20way_mammalian_rankscore, SiPhy_29way_logOdds, SiPhy_29way_logOdds_rankscore, Interpro_domain, GTEx_V6p_gene, GTEx_V6p_tissue, gnomAD_genome_ALL, gnomAD_genome_AFR, gnomAD_genome_AMR, gnomAD_genome_ASJ, gnomAD_genome_EAS, gnomAD_genome_FIN, gnomAD_genome_NFE, gnomAD_genome_OTH, CLNALLELEID, CLNDN, CLNDISDB, CLNREVSTAT, CLNSIG, cosmic70, ICGC_Id, ICGC_Occurrence, InterVar_automated, PVS1, PS1, PS2, PS3, PS4, PM1, PM2, PM3, PM4, PM5, PM6, PP1, PP2, PP3, PP4, PP5, BA1, BS1, BS2, BS3, BS4, BP1, BP2, BP3, BP4, BP5, BP6, BP7, Otherinfo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try {
                connection.setAutoCommit(false);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                for (int i = 0; i < lines.length; i++) {
                    // 跳过空行、表头行（以 "Chr" 开头）和注释行（以 "#" 开头）
                    if (lines[i] == null || lines[i].isBlank() || lines[i].startsWith("Chr\t")
                            || lines[i].startsWith("#"))
                        continue;
                    String[] split = lines[i].split("\\t", -1);
                    // 跳过列数不足的行，防止 ArrayIndexOutOfBoundsException
                    // if (split.length < 153) {
                    // log.warn("Skipping line {}: only {} columns", i + 1, split.length);
                    // continue;
                    // }
                    preparedStatement.setInt(1, sampleId);
                    for (int j = 1; j <= 153; j++) {
                        String value = j <= split.length ? split[j - 1] : ".";
                        preparedStatement.setString(j + 1, value);
                    }
                    StringJoiner otherInfo = new StringJoiner("\t");
                    for (int j = 154; j <= split.length; j++) {
                        otherInfo.add(split[j - 1]);
                    }
                    preparedStatement.setString(155, otherInfo.toString());
                    preparedStatement.addBatch();
                    if (i % 1000 == 0) {
                        preparedStatement.executeBatch();
                        connection.commit();
                    }
                }
                preparedStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 返回样本中功能性变异，包含GT信息
     * 解析Otherinfo列中的VCF FORMAT字段，提取基因型
     */
    public Map<String, List<VariantWithGT>> getVariantsWithGT(int sampleId) {
        String sql = "SELECT `Gene.refGene`, avsnp150, `Chr`, Start, End, Ref, Alt, Otherinfo FROM annovar " +
                "WHERE sample_id = ? " +
                "AND `Func.refGene` = 'exonic' " +
                "AND `ExonicFunc.refGene` != 'synonymous SNV' " +
                "AND avsnp150 IS NOT NULL AND avsnp150 != '.'";

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
                    // TODO: 含分号的多基因注释（如 GENE1;GENE2）暂跳过，Phase 2 处理
                    if (gene.contains(";"))
                        continue;

                    // 获取其他字段
                    String chr = rs.getString(3);
                    int start = rs.getInt(4);
                    int end = rs.getInt(5);
                    String ref = rs.getString(6);
                    String alt = rs.getString(7);
                    String otherInfo = rs.getString(8);

                    // 从otherInfo中提取GT信息
                    String gt = extractGTFromOtherInfo(otherInfo);

                    VariantWithGT variant = new VariantWithGT(gene, rsid, gt);
                    variant.setChr(chr);
                    variant.setStart(start);
                    variant.setEnd(end);
                    variant.setRef(ref);
                    variant.setAlt(alt);

                    geneVariants.computeIfAbsent(gene, k -> new ArrayList<>()).add(variant);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return geneVariants;
    }

    /**
     * 返回样本中功能性变异的 rsID，按基因分组，支持GT信息解析。
     * 过滤条件：exonic 区域、非同义突变、avsnp150 不为 '.' 或空。
     * 增强：可选择是否包含GT信息
     */
    public Map<String, List<Object>> getRsIdsPerGeneWithGT(int sampleId, boolean includeGT) {
        if (!includeGT) {
            // 保持原有行为，返回rsID列表
            String sql = "SELECT `Gene.refGene`, avsnp150 FROM annovar " +
                    "WHERE sample_id = ? " +
                    "AND `Func.refGene` = 'exonic' " +
                    "AND `ExonicFunc.refGene` != 'synonymous SNV' " +
                    "AND avsnp150 IS NOT NULL AND avsnp150 != '.'";

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
                        // TODO: 含分号的多基因注释（如 GENE1;GENE2）暂跳过，Phase 2 处理
                        if (gene.contains(";"))
                            continue;
                        geneRsIds.computeIfAbsent(gene, k -> new ArrayList<>()).add(rsid);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            // 将Map<String, List<String>>转换为Map<String, List<Object>>
            Map<String, List<Object>> result = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : geneRsIds.entrySet()) {
                List<Object> objectList = new ArrayList<>();
                objectList.addAll(entry.getValue());
                result.put(entry.getKey(), objectList);
            }
            return result;
        } else {
            // 包含GT信息的新行为
            String sql = "SELECT `Gene.refGene`, avsnp150, Otherinfo FROM annovar " +
                    "WHERE sample_id = ? " +
                    "AND `Func.refGene` = 'exonic' " +
                    "AND `ExonicFunc.refGene` != 'synonymous SNV' " +
                    "AND avsnp150 IS NOT NULL AND avsnp150 != '.'";

            Map<String, List<Object>> geneVariants = new HashMap<>();
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

                        String otherInfo = rs.getString(3);
                        String gt = extractGTFromOtherInfo(otherInfo);

                        // 创建包含rsid和GT的对象数组
                        Object[] variantData = { rsid, gt };

                        geneVariants.computeIfAbsent(gene, k -> new ArrayList<>()).add(variantData);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            return geneVariants;
        }
    }

    /**
     * 从Otherinfo字符串中提取GT信息
     * 
     * @param otherInfo 包含VCF格式信息的字符串
     * @return 提取的GT信息，如果未找到则返回null
     */
    private String extractGTFromOtherInfo(String otherInfo) {
        if (otherInfo == null || otherInfo.isEmpty()) {
            return null;
        }

        // 查找GT字段模式：可能是 "GT:..." 或者类似 "GT:AD:DP:GD:GL:GQ:OG
        // 0|1:2,1:2:.:-3.95,-0.60,-3.69:32.52:./." 的格式
        // 首先查找FORMAT信息和对应的值

        // 如果otherInfo中包含GT相关的格式，如 "GT:AD:DP..." 后跟 "0|1:2,1:..."
        String[] parts = otherInfo.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            String formatStr = parts[i];
            String valueStr = parts[i + 1];

            if (formatStr.startsWith("GT:") || formatStr.equals("GT")) {
                // 提取GT值，通常是冒号分隔的值中的第一个
                String[] values = valueStr.split(":");
                if (values.length > 0) {
                    String gtValue = values[0];
                    // 验证是否是有效的GT格式（如 0|1, 1|0, 0/0, 1|1 等）
                    if (isValidGTFormat(gtValue)) {
                        return gtValue;
                    }
                }
            }
        }

        // 查找直接的GT模式，如 "GT" 后面跟着基因型
        Pattern gtPattern = Pattern.compile("GT\\s+([0-9\\|\\/]+)");
        Matcher matcher = gtPattern.matcher(otherInfo);
        if (matcher.find()) {
            String gtValue = matcher.group(1);
            if (isValidGTFormat(gtValue)) {
                return gtValue;
            }
        }

        // 查找直接的基因型模式
        Pattern directGT = Pattern.compile("(?:^|\\s)([0-9][\\|\\/][0-9])(?:\\s|$)");
        Matcher directMatcher = directGT.matcher(otherInfo);
        if (directMatcher.find()) {
            String gtValue = directMatcher.group(1);
            if (isValidGTFormat(gtValue)) {
                return gtValue;
            }
        }

        return null;
    }

    /**
     * 验证是否是有效的GT格式
     * 
     * @param gt GT字符串
     * @return 是否有效
     */
    private boolean isValidGTFormat(String gt) {
        if (gt == null)
            return false;
        // 检查GT格式，如 0|0, 0|1, 1|0, 1|1, 0/0, 0/1 等
        return gt.matches("[0-9][\\|\\/][0-9]");
    }

    /**
     * 返回样本中功能性变异的 rsID，按基因分组。
     * 过滤条件：exonic 区域、非同义突变、avsnp150 不为 '.' 或空。
     * 注意：Gene.refGene 含分号（如 GENE1;GENE2）时暂不拆分，作为 TODO 记录。
     */
    public Map<String, List<String>> getRsIdsPerGene(int sampleId) {
        String sql = "SELECT `Gene.refGene`, avsnp150 FROM annovar " +
                "WHERE sample_id = ? " +
                "AND `Func.refGene` = 'exonic' " +
                "AND `ExonicFunc.refGene` != 'synonymous SNV' " +
                "AND avsnp150 IS NOT NULL AND avsnp150 != '.'";
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
                    // TODO: 含分号的多基因注释（如 GENE1;GENE2）暂跳过，Phase 2 处理
                    if (gene.contains(";"))
                        continue;
                    geneRsIds.computeIfAbsent(gene, k -> new ArrayList<>()).add(rsid);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return geneRsIds;
    }

    public List<String> getRefGenes(int sampleId) {
        String sql = "select distinct `Gene.refGene` from annovar where `ExonicFunc.refGene` != 'synonymous SNV' and sample_id = ?";
        List<String> genes = new ArrayList<>();
        DBUtils.execSQL(connection -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(sql);
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
