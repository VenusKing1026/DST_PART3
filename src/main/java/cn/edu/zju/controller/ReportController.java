package cn.edu.zju.controller;

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

            document.add(new Paragraph("Matching Report"));
            document.add(new Paragraph("Sample ID: " + sampleId));
            document.add(new Paragraph("Matched Result Count: " + results.size()));
            document.add(new Paragraph(" "));

            for (int i = 0; i < results.size(); i++) {
                MatchingResult item = results.get(i);
                document.add(new Paragraph((i + 1) + ". Drug: " + item.getDrugName()));
                document.add(new Paragraph("Source: " + item.getSource()));
                document.add(new Paragraph("Summary: " + item.getSummaryMarkdown()));
                document.add(new Paragraph(" "));
            }
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF report", e);
        } finally {
            document.close();
        }
    }
}