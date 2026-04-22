package cn.edu.zju.controller;
import cn.edu.zju.service.AnnovarValidator;
import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DrugLabelDao;
import cn.edu.zju.dao.SampleDao;
import cn.edu.zju.service.AnnovarService;
import cn.edu.zju.service.VcfParser;
import cn.edu.zju.servlet.DispatchServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private final SampleDao sampleDao = new SampleDao();
    private final AnnovarDao annovarDao = new AnnovarDao();
    private final DrugLabelDao drugLabelDao = new DrugLabelDao();

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerPostMapping("/uploadAnnovar", this::uploadAnnovarOutput);
        dispatcher.registerPostMapping("/uploadVcf", this::uploadVcf);
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
            response.sendRedirect("samples");
            return;
        }

        Integer sampleId;
        try {
            sampleId = Integer.valueOf(sampleIdParameter);
        } catch (NumberFormatException e) {
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
        request.setAttribute("sample", sampleDao.findById(sampleId));
        request.getRequestDispatcher("/views/matching_index_search.jsp").forward(request, response);
    }

    private List<DrugLabel> doMatch(List<String> refGenes, List<DrugLabel> drugLabels) {
        List<DrugLabel> matchedLabels = new ArrayList<>();
        for (DrugLabel drugLabel : drugLabels) {
            boolean matched = false;
            for (String gene : refGenes) {
                if (drugLabel.getSummaryMarkdown() != null && drugLabel.getSummaryMarkdown().contains(gene)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                matchedLabels.add(drugLabel);
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
        if (requestPart == null || requestPart.getSize() == 0) {
            request.setAttribute("validateError", "ANNOVAR output file can not be blank");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        String content = new String(requestPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        try {
            AnnovarValidator.validateMultianno(content);

            int sampleId = sampleDao.save(uploadedBy);
            annovarDao.save(sampleId, content);

            response.sendRedirect("matching?sampleId=" + sampleId);
        } catch (Exception e) {
            request.setAttribute("validateError", "ANNOVAR output file is invalid: " + e.getMessage());
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
        }
    }

    public void uploadVcf(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uploadedBy = request.getParameter("uploaded_by");
        if (uploadedBy == null || uploadedBy.isBlank()) {
            request.setAttribute("validateError", "Uploaded by can not be blank");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        Part requestPart = request.getPart("vcf");
        if (requestPart == null || requestPart.getSize() == 0) {
            request.setAttribute("validateError", "VCF file can not be blank");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "haining_biomed_upload");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String submittedName = getSubmittedFileName(requestPart);
        if (submittedName == null || !submittedName.toLowerCase().endsWith(".vcf")) {
            request.setAttribute("validateError", "Only .vcf file is supported");
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }

        File vcfFile = new File(tempDir, System.currentTimeMillis() + "_" + submittedName);
        try (InputStream in = requestPart.getInputStream();
             OutputStream out = new FileOutputStream(vcfFile)) {
            in.transferTo(out);
        }

        try {
            VcfParser.validateVcf(vcfFile);

            File annovarResult = AnnovarService.runAnnovar(vcfFile, true);
            String content = Files.readString(annovarResult.toPath(), StandardCharsets.UTF_8);

            AnnovarValidator.validateMultianno(content);

            int sampleId = sampleDao.save(uploadedBy);
            annovarDao.save(sampleId, content);

            response.sendRedirect("matching?sampleId=" + sampleId);
        } catch (Exception e) {
            log.error("VCF upload / annotation failed", e);
            request.setAttribute("validateError", "VCF annotation failed: " + e.getMessage());
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
        }
    }

    private String getSubmittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}