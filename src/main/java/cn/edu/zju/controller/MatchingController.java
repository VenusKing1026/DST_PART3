package cn.edu.zju.controller;

import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DrugLabelDao;
import cn.edu.zju.dao.SampleDao;
import cn.edu.zju.dbutils.DBUtils;
import cn.edu.zju.servlet.DispatchServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);

    private SampleDao sampleDao = new SampleDao();
    private AnnovarDao annovarDao = new AnnovarDao();
    private DrugLabelDao drugLabelDao = new DrugLabelDao();

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
    private void handleAnnovarFile(Part filePart, int sampleId) throws IOException {
        InputStream inputStream = filePart.getInputStream();
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes);
        annovarDao.save(sampleId, content);
    }
    private List<String> handleVcfFile(Part filePart, int sampleId) throws IOException {
        Set<String> genes = new LinkedHashSet<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(filePart.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length < 8) {
                    continue;
                }

                String chr = parts[0];
                String pos = parts[1];
                String ref = parts[3];
                String alt = parts[4];
                String info = parts[7];

                log.info("VCF record: sampleId={}, chr={}, pos={}, ref={}, alt={}", sampleId, chr, pos, ref, alt);

                genes.addAll(extractGenesFromInfo(info));
            }
        }

        if (genes.isEmpty()) {
            throw new IllegalArgumentException(
                    "This VCF does not contain gene annotations. Please upload an annotated VCF or ANNOVAR output."
            );
        }

        return new ArrayList<>(genes);
    }

    private List<String> extractGenesFromInfo(String info) {
        Set<String> genes = new LinkedHashSet<>();

        if (info == null || info.isEmpty()) {
            return new ArrayList<>(genes);
        }

        String[] fields = info.split(";");

        for (String field : fields) {
            if (field.startsWith("ANN=")) {
                String annValue = field.substring(4);
                String[] annItems = annValue.split(",");

                for (String ann : annItems) {
                    String[] tokens = ann.split("\\|");
                    if (tokens.length > 3) {
                        String gene = tokens[3].trim();
                        if (!gene.isEmpty() && !".".equals(gene)) {
                            genes.add(gene);
                        }
                    }
                }
            } else if (field.startsWith("GENEINFO=")) {
                String geneInfo = field.substring("GENEINFO=".length());
                String[] geneItems = geneInfo.split("\\|");

                for (String geneItem : geneItems) {
                    String gene = geneItem.split(":")[0].trim();
                    if (!gene.isEmpty() && !".".equals(gene)) {
                        genes.add(gene);
                    }
                }
            } else if (field.startsWith("GENE=")) {
                String geneValue = field.substring("GENE=".length());
                String[] geneItems = geneValue.split(",");

                for (String gene : geneItems) {
                    gene = gene.trim();
                    if (!gene.isEmpty() && !".".equals(gene)) {
                        genes.add(gene);
                    }
                }
            }
        }

        return new ArrayList<>(genes);
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
        sample.setParseStatus("pending");

        int sampleId = sampleDao.save(sample);

        log.info("sampleId = {}", sampleId);
        log.info("inputType = {}", inputType);
        log.info("uploadedBy = {}", uploadedBy);
        log.info("fileName = {}", fileName);

        try {
            if ("annovar".equalsIgnoreCase(inputType)) {
                handleAnnovarFile(filePart, sampleId);
                log.info("ANNOVAR upload selected");
                response.sendRedirect("matching?sampleId=" + sampleId);
                return;
            } else if ("vcf".equalsIgnoreCase(inputType)) {
                List<String> refGenes = handleVcfFile(filePart, sampleId);
                List<DrugLabel> allDrugLabels = drugLabelDao.findAll();
                List<DrugLabel> matchedLabels = doMatch(refGenes, allDrugLabels);

                request.setAttribute("sampleId", sampleId);
                request.setAttribute("refGenes", refGenes);
                request.setAttribute("matchedLabels", matchedLabels);
                request.getRequestDispatcher("/views/matching_index_search.jsp").forward(request, response);
                return;
            } else {
                request.setAttribute("error", "Unsupported input type: " + inputType);
                request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
                return;
            }
        } catch (Exception e) {
            log.error("File processing failed", e);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/views/matching_index_error.jsp").forward(request, response);
            return;
        }
    }

}
