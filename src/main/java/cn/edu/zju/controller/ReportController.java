package cn.edu.zju.controller;

import cn.edu.zju.bean.DosingGuideline;
import cn.edu.zju.bean.MatchingResult;
import cn.edu.zju.dao.MatchingResultDao;
import cn.edu.zju.servlet.DispatchServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class ReportController {

    private MatchingResultDao matchingResultDao = new MatchingResultDao();

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerGetMapping("/downloadPdf", this::downloadPdf);
    }

    public void downloadPdf(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String sampleIdParameter = request.getParameter("sampleId");
        if (sampleIdParameter == null) {
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

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=report-sample-" + sampleId + ".pdf");

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            document.add(new Paragraph("PGx Matching Report"));
            document.add(new Paragraph("Sample ID: " + sampleId));
            document.add(new Paragraph("Gene Count: " + results.size()));
            document.add(new Paragraph(" "));

            for (int i = 0; i < results.size(); i++) {
                MatchingResult result = results.get(i);
                document.add(new Paragraph((i + 1) + ". Gene: " + result.getGene()));
                document.add(new Paragraph("   Diplotype: " + result.getDiplotype()));
                document.add(new Paragraph("   Phenotype: " + result.getPhenotype()));

                if (result.getMetabolizerMatches() != null && !result.getMetabolizerMatches().isEmpty()) {
                    document.add(new Paragraph("   Metabolizer-specific Guidelines:"));
                    for (DosingGuideline g : result.getMetabolizerMatches()) {
                        document.add(new Paragraph("     - " + g.getName() + " (" + g.getSource() + "): " + g.getSummaryMarkdown()));
                    }
                }

                if (result.getGeneralMatches() != null && !result.getGeneralMatches().isEmpty()) {
                    document.add(new Paragraph("   General Drug Information:"));
                    for (DosingGuideline g : result.getGeneralMatches()) {
                        document.add(new Paragraph("     - " + g.getName() + " (" + g.getSource() + "): " + g.getSummaryMarkdown()));
                    }
                }

                document.add(new Paragraph(" "));
            }
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF report", e);
        } finally {
            document.close();
        }
    }
}
