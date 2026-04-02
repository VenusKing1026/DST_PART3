package cn.edu.zju.controller;

import cn.edu.zju.bean.DosingGuideline;
import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Genotype;
import cn.edu.zju.bean.MatchingResult;
import cn.edu.zju.bean.Phenotype;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DosingGuidelineDao;
import cn.edu.zju.dao.DrugLabelDao;
import cn.edu.zju.dao.GenotypeDao;
import cn.edu.zju.dao.PhenotypeDao;
import cn.edu.zju.dao.SampleDao;
import cn.edu.zju.servlet.DispatchServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private SampleDao sampleDao = new SampleDao();
    private AnnovarDao annovarDao = new AnnovarDao();
    private DrugLabelDao drugLabelDao = new DrugLabelDao();
    private GenotypeDao genotypeDao = new GenotypeDao();
    private PhenotypeDao phenotypeDao = new PhenotypeDao();
    private DosingGuidelineDao dosingGuidelineDao = new DosingGuidelineDao();

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerPostMapping("/upload", this::uploadAnnovarOutput);
        dispatcher.registerGetMapping("/matchingIndex", this::matchingIndex);
        dispatcher.registerGetMapping("/matching", this::matching);
        dispatcher.registerGetMapping("/samples", this::samples);

    }

    public void matchingIndex(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.getRequestDispatcher("/views/matching_index.jsp").forward(request, response);
    }

    public void samples(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        List<Sample> samples = sampleDao.findAll();
        request.setAttribute("samples", samples);
        request.getRequestDispatcher("/views/samples.jsp").forward(request, response);
    }

    public void matching(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String sampleIdParameter = request.getParameter("sampleId");
        if (sampleIdParameter == null) {
            request.getRequestDispatcher("/views/samples.jsp").forward(request, response);
            return;
        }
        Integer sampleId = null;
        try {
            sampleId = Integer.valueOf(sampleIdParameter);
        } catch (NumberFormatException e) {
            response.sendRedirect("samples");
            return;
        }

        // legacy mode fallback
        if ("legacy".equals(request.getParameter("mode"))) {
            List<String> refGenes = annovarDao.getRefGenes(sampleId);
            if (refGenes.isEmpty()) { response.sendRedirect("samples"); return; }
            List<DrugLabel> matched = doMatchLegacy(refGenes, drugLabelDao.findAll());
            request.setAttribute("matched", matched);
            request.setAttribute("sample", sampleDao.findById(sampleId));
            request.getRequestDispatcher("/views/matching_index_search.jsp").forward(request, response);
            return;
        }

        // PGx pipeline
        List<MatchingResult> matchingResults = runPgxPipeline(sampleId);
        sampleDao.updateMatchingStatus(sampleId, "completed");

        request.setAttribute("matchingResults", matchingResults);
        request.setAttribute("sample", sampleDao.findById(sampleId));
        request.getRequestDispatcher("/views/matching_index_search.jsp").forward(request, response);
    }

    /**
     * PGx 匹配流水线：
     * annovar rsID -> star allele -> diplotype -> phenotype -> dosing guideline
     */
    private List<MatchingResult> runPgxPipeline(int sampleId) {
        // Step 1: 获取功能性 rsID（按基因分组）
        Map<String, List<String>> geneRsIds = annovarDao.getRsIdsPerGene(sampleId);
        log.info("[PGx] sampleId={} | Step1: found {} genes with functional rsIDs: {}",
                sampleId, geneRsIds.size(), geneRsIds.keySet());

        List<MatchingResult> results = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : geneRsIds.entrySet()) {
            String gene = entry.getKey();
            List<String> rsids = entry.getValue();
            log.info("[PGx] gene={} | rsIDs: {}", gene, rsids);

            // Step 2: rsID -> star allele（V1: 取第一条）
            List<Genotype> genotypes = genotypeDao.findFirstByRsIds(rsids);
            if (genotypes.isEmpty()) {
                log.info("[PGx] gene={} | Step2: no star allele found, skipping", gene);
                continue;
            }
            log.info("[PGx] gene={} | Step2: star alleles: {}",
                    gene, genotypes.stream().map(Genotype::getStarAllele).collect(Collectors.toList()));

            // Step 3: 构建 diplotype
            String diplotype = buildDiplotype(genotypes);
            log.info("[PGx] gene={} | Step3: diplotype={}", gene, diplotype);

            // Step 4: diplotype -> phenotype
            String phenotype = "Indeterminate";
            Phenotype pt = phenotypeDao.findByGeneDiplotype(gene, diplotype);
            if (pt != null) {
                phenotype = pt.getPhenotype();
            }
            log.info("[PGx] gene={} | Step4: phenotype={}", gene, phenotype);

            // Step 5: 按基因名查 dosing guideline，按 metabolizer 分类
            List<DosingGuideline> all = dosingGuidelineDao.findByGeneContains(gene);
            List<DosingGuideline> metabolizerMatches = all.stream()
                    .filter(g -> g.getSummaryMarkdown() != null &&
                                 g.getSummaryMarkdown().toLowerCase().contains("metabolizer"))
                    .collect(Collectors.toList());
            List<DosingGuideline> generalMatches = all.stream()
                    .filter(g -> g.getSummaryMarkdown() == null ||
                                 !g.getSummaryMarkdown().toLowerCase().contains("metabolizer"))
                    .collect(Collectors.toList());
            log.info("[PGx] gene={} | Step5: metabolizerMatches={}, generalMatches={}",
                    gene, metabolizerMatches.size(), generalMatches.size());

            results.add(new MatchingResult(gene, diplotype, phenotype, metabolizerMatches, generalMatches));
        }

        log.info("[PGx] sampleId={} | pipeline complete, {} gene results", sampleId, results.size());
        return results;
    }

    /**
     * 从 genotype 列表构建 diplotype 字符串。
     * - 1 个 star allele -> *1/[star]
     * - 2+ 个 star allele -> [star1]/[star2]（数字小的在前）
     */
    private String buildDiplotype(List<Genotype> genotypes) {
        if (genotypes.size() == 1) {
            return canonicalize("*1", genotypes.get(0).getStarAllele());
        }
        return canonicalize(genotypes.get(0).getStarAllele(), genotypes.get(1).getStarAllele());
    }

    /**
     * 将两个 star allele 规范化为 小号/大号 格式（如 *1/*2 而非 *2/*1）。
     * 提取数字部分比较，解析失败则按字母序排列。
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

    /** 原有文本匹配逻辑，保留为 legacy 回退路径（?mode=legacy） */
    private List<DrugLabel> doMatchLegacy(List<String> refGenes, List<DrugLabel> drugLabels) {
        List<DrugLabel> matchedLabels = new ArrayList<>();
        for (DrugLabel drugLabel : drugLabels) {
            for (String gene : refGenes) {
                if (drugLabel.getSummaryMarkdown() != null &&
                        drugLabel.getSummaryMarkdown().contains(gene)) {
                    matchedLabels.add(drugLabel);
                    break;
                }
            }
        }
        return matchedLabels;
    }

    public void uploadAnnovarOutput(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uploadedBy = request.getParameter("uploaded_by");
        if (uploadedBy == null || uploadedBy.isBlank()) {
            request.setAttribute("validateError", "Uploaded by can not be blank");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }
        Part requestPart = request.getPart("annovar");
        if (requestPart == null) {
            request.setAttribute("validateError", "annovar output file can not be blank");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }
        InputStream inputStream = requestPart.getInputStream();
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes);
        int sampleId = sampleDao.save(uploadedBy);
        try {
            annovarDao.save(sampleId, content);
        } catch (ArrayIndexOutOfBoundsException e) {
            request.setAttribute("validateError", "annovar output file is invalid");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }
        response.sendRedirect("matching?sampleId=" + sampleId);
    }
}
