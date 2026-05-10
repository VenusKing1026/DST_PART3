package cn.edu.zju.controller;

import cn.edu.zju.bean.DosingGuideline;
import cn.edu.zju.bean.MatchingResult;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.MatchingResultDao;
import cn.edu.zju.dao.SampleDao;
import cn.edu.zju.servlet.DispatchServlet;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ReportController {

    private final MatchingResultDao matchingResultDao = new MatchingResultDao();
    private final SampleDao sampleDao = new SampleDao();

    private static final Color PRIMARY = new Color(28, 78, 128);
    private static final Color LIGHT_BLUE = new Color(232, 243, 251);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color BORDER = new Color(210, 210, 210);
    private static final Color RED_TITLE = new Color(196, 68, 68);
    private static final Color TEXT_DARK = new Color(50, 50, 50);

    // 只改字体：统一字体常量
    private static final Font TITLE_FONT = new Font(Font.TIMES_ROMAN, 20, Font.BOLD, PRIMARY);
    private static final Font SUBTITLE_FONT = new Font(Font.TIMES_ROMAN, 10, Font.ITALIC, Color.GRAY);
    private static final Font BLOCK_TITLE_FONT = new Font(Font.TIMES_ROMAN, 14, Font.BOLD, PRIMARY);
    private static final Font SECTION_TITLE_FONT_RED = new Font(Font.TIMES_ROMAN, 11.5f, Font.BOLD, RED_TITLE);
    private static final Font SECTION_TITLE_FONT_GRAY = new Font(Font.TIMES_ROMAN, 11.5f, Font.BOLD, new Color(90, 90, 90));
    private static final Font GENE_FONT = new Font(Font.TIMES_ROMAN, 13, Font.BOLD, TEXT_DARK);
    private static final Font LABEL_FONT = new Font(Font.TIMES_ROMAN, 10, Font.BOLD, TEXT_DARK);
    private static final Font VALUE_FONT = new Font(Font.TIMES_ROMAN, 10, Font.NORMAL, TEXT_DARK);
    private static final Font HEADER_FONT = new Font(Font.TIMES_ROMAN, 10, Font.BOLD, Color.BLACK);
    private static final Font BODY_FONT = new Font(Font.TIMES_ROMAN, 9.5f, Font.NORMAL, TEXT_DARK);
    private static final Font SUMMARY_FONT = new Font(Font.TIMES_ROMAN, 10.5f, Font.NORMAL, TEXT_DARK);
    private static final Font DISCLAIMER_FONT = new Font(Font.TIMES_ROMAN, 11.5f, Font.NORMAL, TEXT_DARK);

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerGetMapping("/downloadPdf", this::downloadPdf);
    }

    public void downloadPdf(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String sampleIdParameter = request.getParameter("sampleId");
        if (sampleIdParameter == null || sampleIdParameter.isBlank()) {
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("sampleId is required");
            return;
        }

        Integer sampleId;
        try {
            sampleId = Integer.valueOf(sampleIdParameter);
        } catch (NumberFormatException e) {
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("invalid sampleId");
            return;
        }

        List<MatchingResult> results = matchingResultDao.findBySampleId(sampleId);
        Sample sample = sampleDao.findById(sampleId);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=pgx-report-sample-" + sampleId + ".pdf");

        Document document = new Document(PageSize.A4, 36, 36, 42, 42);

        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            addReportHeader(document);
            addSampleInformation(document, sample, sampleId, results);
            addExecutiveSummary(document, results);

            for (int i = 0; i < results.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                addGeneSection(document, results.get(i));
            }

            if (!results.isEmpty()) {
                document.newPage();
            }
            addDisclaimerSection(document);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF report", e);
        } finally {
            document.close();
        }
    }

    private void addReportHeader(Document document) throws DocumentException {
        Paragraph title = new Paragraph("Clinical Pharmacogenomics Matching Report", TITLE_FONT);
        title.setSpacingAfter(6f);
        document.add(title);

        String generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Paragraph sub = new Paragraph("Generated at: " + generatedAt, SUBTITLE_FONT);
        sub.setSpacingAfter(16f);
        document.add(sub);
    }

    private void addSampleInformation(Document document, Sample sample, Integer sampleId, List<MatchingResult> results) throws DocumentException {
        addBlockTitle(document, "Sample Information", PRIMARY);

        PdfPTable table = new PdfPTable(new float[]{1.3f, 2.2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(14f);

        String createdAtText = "";
        if (sample != null && sample.getCreatedAt() != null) {
            createdAtText = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sample.getCreatedAt());
        }

        addInfoRow(table, "Sample ID", String.valueOf(sampleId));
        addInfoRow(table, "Uploaded By", sample != null ? safe(sample.getUploadedBy()) : "");
        addInfoRow(table, "Created At", createdAtText);
        addInfoRow(table, "Input Type", sample != null ? safe(sample.getInputType()) : "");
        addInfoRow(table, "File Name", sample != null ? safe(sample.getFileName()) : "");
        addInfoRow(table, "Parse Status", sample != null ? safe(sample.getParseStatus()) : "");
        addInfoRow(table, "Matching Status", sample != null ? safe(sample.getMatchingStatus()) : "");
        addInfoRow(table, "Gene Count", String.valueOf(results.size()));

        document.add(table);
    }

    private void addExecutiveSummary(Document document, List<MatchingResult> results) throws DocumentException {
        addBlockTitle(document, "Executive Summary", PRIMARY);

        int metabolizerCount = results.stream()
                .mapToInt(r -> r.getMetabolizerMatches() == null ? 0 : r.getMetabolizerMatches().size())
                .sum();

        int generalCount = results.stream()
                .mapToInt(r -> r.getGeneralMatches() == null ? 0 : r.getGeneralMatches().size())
                .sum();

        String genes = results.isEmpty()
                ? "None"
                : results.stream().map(MatchingResult::getGene).collect(Collectors.joining(", "));

        Paragraph p1 = new Paragraph(
                "This report summarizes pharmacogenomics matching results for the current sample. " +
                        "A total of " + results.size() + " pharmacogene section(s) were identified.",
                SUMMARY_FONT
        );
        p1.setSpacingAfter(6f);

        Paragraph p2 = new Paragraph(
                "Genes included: " + genes + ". " +
                        "Metabolizer-specific guideline entries: " + metabolizerCount + ". " +
                        "General drug information entries: " + generalCount + ".",
                SUMMARY_FONT
        );
        p2.setSpacingAfter(14f);

        document.add(p1);
        document.add(p2);
    }

    private void addGeneSection(Document document, MatchingResult result) throws DocumentException {
        addGeneBanner(document, result);

        PdfPTable infoTable = new PdfPTable(new float[]{1.0f, 1.8f, 1.0f, 1.8f});
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(10f);

        addMiniInfoCell(infoTable, "Diplotype", true);
        addMiniInfoCell(infoTable, safe(result.getDiplotype()), false);
        addMiniInfoCell(infoTable, "Phenotype", true);
        addMiniInfoCell(infoTable, safe(result.getPhenotype()), false);

        document.add(infoTable);

        List<DosingGuideline> metabolizerMatches = result.getMetabolizerMatches();
        List<DosingGuideline> generalMatches = result.getGeneralMatches();

        if (metabolizerMatches != null && !metabolizerMatches.isEmpty()) {
            addSectionSubtitle(document,
                    "Metabolizer-specific Guidelines (" + metabolizerMatches.size() + ")",
                    RED_TITLE);
            addGuidelineTable(document, metabolizerMatches);
        }

        if (generalMatches != null && !generalMatches.isEmpty()) {
            addSectionSubtitle(document,
                    "General Drug Information (" + generalMatches.size() + ")",
                    new Color(90, 90, 90));
            addGuidelineTable(document, generalMatches);
        }

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(8f);
        document.add(spacer);
    }

    private void addGeneBanner(Document document, MatchingResult result) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingBefore(4f);
        banner.setSpacingAfter(8f);

        PdfPCell cell = new PdfPCell(new Phrase(safe(result.getGene()), GENE_FONT));
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorderColor(BORDER);
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        banner.addCell(cell);

        document.add(banner);
    }

    private void addGuidelineTable(Document document, List<DosingGuideline> guidelines) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 2.6f, 1.8f, 4.6f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        table.setHeaderRows(1);

        addHeaderCell(table, "#");
        addHeaderCell(table, "Name");
        addHeaderCell(table, "Source");
        addHeaderCell(table, "Summary");

        int index = 1;
        for (DosingGuideline g : guidelines) {
            addBodyCell(table, String.valueOf(index++), Element.ALIGN_CENTER);
            addBodyCell(table, safe(g.getName()), Element.ALIGN_LEFT);
            addBodyCell(table, safe(g.getSource()), Element.ALIGN_LEFT);
            addBodyCell(table, cleanHtml(safe(g.getSummaryMarkdown())), Element.ALIGN_LEFT);
        }

        document.add(table);
    }

    private void addDisclaimerSection(Document document) throws DocumentException {
        addBlockTitle(document, "Disclaimer", PRIMARY);

        Paragraph p1 = new Paragraph(
                "This report is provided for clinical reference only and is not a final prescription.",
                DISCLAIMER_FONT
        );
        p1.setSpacingAfter(5f);

        Paragraph p2 = new Paragraph(
                "Clinical decisions should be made by qualified healthcare professionals based on the patient's full medical condition, including age, body weight, liver and kidney function, concomitant medications, and treatment history.",
                DISCLAIMER_FONT
        );
        p2.setSpacingAfter(5f);

        Paragraph p3 = new Paragraph(
                "The interpretation in this report is limited to the current testing scope and the evidence available at the time of report generation.",
                DISCLAIMER_FONT
        );
        p3.setSpacingAfter(5f);

        Paragraph p4 = new Paragraph(
                "Therapeutic efficacy and adverse reactions should be monitored during treatment, and dosage adjustments should be made when necessary.",
                DISCLAIMER_FONT
        );
        p4.setSpacingAfter(10f);

        document.add(p1);
        document.add(p2);
        document.add(p3);
        document.add(p4);
    }

    private void addBlockTitle(Document document, String title, Color color) throws DocumentException {
        Paragraph p = new Paragraph(title, BLOCK_TITLE_FONT);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(8f);
        document.add(p);
    }

    private void addSectionSubtitle(Document document, String title, Color color) throws DocumentException {
        Font font = color.equals(RED_TITLE) ? SECTION_TITLE_FONT_RED : SECTION_TITLE_FONT_GRAY;
        Paragraph p = new Paragraph(title, font);
        p.setSpacingBefore(2f);
        p.setSpacingAfter(6f);
        document.add(p);
    }

    private void addInfoRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, LABEL_FONT));
        keyCell.setBackgroundColor(LIGHT_BLUE);
        keyCell.setBorderColor(BORDER);
        keyCell.setPadding(7f);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(7f);

        table.addCell(keyCell);
        table.addCell(valueCell);
    }

    private void addMiniInfoCell(PdfPTable table, String text, boolean key) {
        Font font = key ? LABEL_FONT : VALUE_FONT;

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER);
        cell.setPadding(6f);
        cell.setBackgroundColor(key ? LIGHT_BLUE : Color.WHITE);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(235, 235, 235));
        cell.setBorderColor(BORDER);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setBorderColor(BORDER);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        table.addCell(cell);
    }

    private String cleanHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .trim();
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}