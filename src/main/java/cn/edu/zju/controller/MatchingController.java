package cn.edu.zju.controller;

import cn.edu.zju.bean.DosingGuideline;
import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Genotype;
import cn.edu.zju.bean.MatchingResult;
import cn.edu.zju.bean.Phenotype;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.bean.ScoredAllele;
import cn.edu.zju.bean.VariantWithGT;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DosingGuidelineDao;
import cn.edu.zju.dao.DrugLabelDao;
import cn.edu.zju.dao.GenotypeDao;
import cn.edu.zju.dao.GenotypeDao.DiplotypeResult;
import cn.edu.zju.dao.MatchingResultDao;
import cn.edu.zju.dao.PhenotypeDao;
import cn.edu.zju.dao.SampleDao;
import cn.edu.zju.service.AnnovarService;
import cn.edu.zju.servlet.DispatchServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MatchingController extends BaseController {   //改动1

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private SampleDao sampleDao = new SampleDao();
    private AnnovarDao annovarDao = new AnnovarDao();
    private DrugLabelDao drugLabelDao = new DrugLabelDao();
    private GenotypeDao genotypeDao = new GenotypeDao();
    private PhenotypeDao phenotypeDao = new PhenotypeDao();
    private DosingGuidelineDao dosingGuidelineDao = new DosingGuidelineDao();
    private AnnovarService annovarService = new AnnovarService();
    private MatchingResultDao matchingResultDao = new MatchingResultDao();

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerPostMapping("/upload", this::uploadVariantFile);
        dispatcher.registerGetMapping("/matchingIndex", this::matchingIndex);
        dispatcher.registerGetMapping("/matching", this::matching);
        dispatcher.registerGetMapping("/samples", this::samples);

    }

    public void matchingIndex(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int userId = getCurrentUserId(request);
        if (userId == -1) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/views/matching_index.jsp").forward(request, response);
    }

    public void samples(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int userId = getCurrentUserId(request);
        if (userId == -1) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        List<Sample> samples = sampleDao.findByUser(userId);  //改动2
        request.setAttribute("samples", samples);
        request.setAttribute("hasActiveStatus", samples.stream().anyMatch(sample ->
                "uploading".equalsIgnoreCase(sample.getParseStatus()) || "processing".equalsIgnoreCase(sample.getParseStatus())));
        request.getRequestDispatcher("/views/samples.jsp").forward(request, response);
    }

    public void matching(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int userId = getCurrentUserId(request);
        if (userId == -1) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
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
        // PGx pipeline (V4.1 chromosome-aware)
        List<MatchingResult> matchingResults = runPgxPipeline(sampleId);
        matchingResultDao.saveAll(sampleId, matchingResults);
        sampleDao.updateMatchingStatus(sampleId, "completed");

        request.setAttribute("matchingResults", matchingResults);
        request.setAttribute("sample", sampleDao.findById(sampleId, userId));
        request.getRequestDispatcher("/views/matching_index_search.jsp").forward(request, response);
    }

    /**
     * PGx 匹配流水线（V4.1 染色体感知版本）：
     * annovar rsID + GT -> 拆分染色体 -> star allele -> diplotype -> phenotype -> dosing guideline
     */
    private List<MatchingResult> runPgxPipeline(int sampleId) {
        // Step 1: 获取功能性变异（按基因分组，含 GT 信息）
        Map<String, List<VariantWithGT>> geneVariants = annovarDao.getVariantsWithGT(sampleId);
        log.info("[PGx V4.1] sampleId={} | Step1: found {} genes with GT info: {}",
                sampleId, geneVariants.size(), geneVariants.keySet());

        List<MatchingResult> results = new ArrayList<>();

        for (Map.Entry<String, List<VariantWithGT>> entry : geneVariants.entrySet()) {
            String gene = entry.getKey();
            List<VariantWithGT> variantsWithGT = entry.getValue();
            log.info("[PGx V4.1] gene={} | variants count={}, rsIDs: {}",
                    gene, variantsWithGT.size(),
                    variantsWithGT.stream().map(VariantWithGT::getRsid).collect(Collectors.toList()));

            // Step 2 & 3: 根据 GT 拆分染色体，分别打分，推断 diplotype
            DiplotypeResult diplotypeResult = genotypeDao.inferDiplotypeWithGT(gene, variantsWithGT);
            String diplotype = diplotypeResult.getDiplotype();
            log.info("[PGx V4.1] gene={} | Step2-3: diplotype={} (allele1={}, allele2={})",
                    gene, diplotype, diplotypeResult.getAllele1(), diplotypeResult.getAllele2());

            // Step 4: diplotype -> phenotype
            String phenotype = "Indeterminate";
            Phenotype pt = phenotypeDao.findByGeneDiplotype(gene, diplotype);
            if (pt != null) {
                phenotype = pt.getPhenotype();
            }
            log.info("[PGx V4.1] gene={} | Step4: phenotype={}", gene, phenotype);

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
            log.info("[PGx V4.1] gene={} | Step5: metabolizerMatches={}, generalMatches={}",
                    gene, metabolizerMatches.size(), generalMatches.size());

            results.add(new MatchingResult(gene, diplotype, phenotype, metabolizerMatches, generalMatches));
        }

        log.info("[PGx V4.1] sampleId={} | pipeline complete, {} gene results", sampleId, results.size());
        return results;
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
    private Path saveUploadedFile(Part filePart, int sampleId, String inputType) throws IOException {
        Path sampleDir = annovarService.createSampleWorkDir(sampleId);
        String suffix = "vcf".equalsIgnoreCase(inputType) ? ".vcf" : ".txt";
        Path uploadedFile = sampleDir.resolve("uploaded" + suffix);
        Files.copy(filePart.getInputStream(), uploadedFile, StandardCopyOption.REPLACE_EXISTING);
        return uploadedFile;
    }

    private void processUploadedFileAsync(int sampleId, String inputType, Path uploadedFile) {
        Thread worker = new Thread(() -> {
            SampleDao workerSampleDao = new SampleDao();
            AnnovarDao workerAnnovarDao = new AnnovarDao();
            AnnovarService workerAnnovarService = new AnnovarService();

            try {
                workerSampleDao.updateParseStatus(sampleId, "processing");
                if ("annovar".equalsIgnoreCase(inputType)) {
                    String content = Files.readString(uploadedFile, StandardCharsets.UTF_8);
                    workerAnnovarDao.save(sampleId, content);
                } else if ("vcf".equalsIgnoreCase(inputType)) {
                    String annovarContent = workerAnnovarService.annotateVcf(uploadedFile, sampleId);
                    workerAnnovarDao.save(sampleId, annovarContent);
                    if (workerAnnovarDao.getRefGenes(sampleId).isEmpty()) {
                        throw new IllegalArgumentException("ANNOVAR completed, but no non-synonymous refGene records were found for matching.");
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported input type: " + inputType);
                }
                workerSampleDao.updateParseStatus(sampleId, "finished");
            } catch (Exception e) {
                log.error("Background file processing failed for sampleId={}", sampleId, e);
                workerSampleDao.updateParseStatus(sampleId, "failed");
            }
        }, "sample-processing-" + sampleId);
        worker.setDaemon(true);
        worker.start();
    }

    public void uploadVariantFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int userId = getCurrentUserId(request);
        if (userId == -1) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String inputType = request.getParameter("input_type");
        String uploadedBy = request.getParameter("uploaded_by");
        Part filePart = request.getPart("variant_file");

        if (inputType == null || inputType.trim().isEmpty()) {
            request.setAttribute("error", "Input type is required.");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        if (uploadedBy == null || uploadedBy.trim().isEmpty()) {
            request.setAttribute("error", "Uploaded by is required.");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Please select a file.");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        Sample sample = new Sample();
        sample.setUserId(userId);
        sample.setCreatedAt(new Date());
        sample.setUploadedBy(uploadedBy);
        sample.setInputType(inputType);
        sample.setFileName(fileName);
        sample.setParseStatus("uploading");

        int sampleId = sampleDao.save(sample);

        log.info("sampleId = {}", sampleId);
        log.info("inputType = {}", inputType);
        log.info("uploadedBy = {}", uploadedBy);
        log.info("fileName = {}", fileName);

        if (!"annovar".equalsIgnoreCase(inputType) && !"vcf".equalsIgnoreCase(inputType)) {
            sampleDao.updateParseStatus(sampleId, "failed");
            request.setAttribute("error", "Unsupported input type: " + inputType);
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        try {
            Path uploadedFile = saveUploadedFile(filePart, sampleId, inputType);
            sampleDao.updateParseStatus(sampleId, "processing");
            processUploadedFileAsync(sampleId, inputType, uploadedFile);
            response.sendRedirect("samples");
        } catch (Exception e) {
            sampleDao.updateParseStatus(sampleId, "failed");
            log.error("File processing failed", e);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
        }
    }

}
