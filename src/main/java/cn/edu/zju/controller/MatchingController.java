package cn.edu.zju.controller;

import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DrugLabelDao;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private SampleDao sampleDao = new SampleDao();
    private AnnovarDao annovarDao = new AnnovarDao();
    private DrugLabelDao drugLabelDao = new DrugLabelDao();
    private AnnovarService annovarService = new AnnovarService();

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerPostMapping("/upload", this::uploadVariantFile);
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
        request.setAttribute("hasActiveStatus", samples.stream().anyMatch(sample ->
                "uploading".equalsIgnoreCase(sample.getParseStatus()) || "processing".equalsIgnoreCase(sample.getParseStatus())));
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
        Sample sample = sampleDao.findById(sampleId);
        if (sample == null) {
            response.sendRedirect("samples");
            return;
        }
        String parseStatus = sample.getParseStatus();
        if (parseStatus != null
                && !parseStatus.trim().isEmpty()
                && !"finished".equalsIgnoreCase(parseStatus)
                && !"pending".equalsIgnoreCase(parseStatus)) {
            response.sendRedirect("samples");
            return;
        }
        List<String> refGenes = annovarDao.getRefGenes(sampleId);
        if (refGenes.isEmpty()) {
            response.sendRedirect("samples");
            return;
        }
        List<DrugLabel> drugLabels = drugLabelDao.findAll();
        List<DrugLabel> matched = doMatch(refGenes, drugLabels);
        request.setAttribute("matched", matched);
        request.setAttribute("sample", sample);
        request.getRequestDispatcher("/views/matching_index_search.jsp").forward(request, response);
    }

    private List<DrugLabel> doMatch(List<String> refGenes, List<DrugLabel> drugLabels) {
        List<DrugLabel> matchedLabels = new ArrayList<>();
        for (DrugLabel drugLabel : drugLabels) {
            boolean matched = false;
            for (String gene: refGenes) {
                if (drugLabel.getSummaryMarkdown().contains(gene)) {
                    matched = true;
                }
            }
            if (matched) {
                matchedLabels.add(drugLabel);
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
