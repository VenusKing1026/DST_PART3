package cn.edu.zju.controller;

import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DosingGuidelineDao;
import cn.edu.zju.dao.DrugLabelDao;
import cn.edu.zju.dao.GenotypeDao;
import cn.edu.zju.dao.PhenotypeDao;
import cn.edu.zju.dao.SampleDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingControllerTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RequestDispatcher requestDispatcher;
    @Mock
    private Part part;

    @Mock
    private SampleDao sampleDao;
    @Mock
    private AnnovarDao annovarDao;
    @Mock
    private DrugLabelDao drugLabelDao;
    @Mock
    private GenotypeDao genotypeDao;
    @Mock
    private PhenotypeDao phenotypeDao;
    @Mock
    private DosingGuidelineDao dosingGuidelineDao;

    private MatchingController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new MatchingController();
        inject("sampleDao", sampleDao);
        inject("annovarDao", annovarDao);
        inject("drugLabelDao", drugLabelDao);
        inject("genotypeDao", genotypeDao);
        inject("phenotypeDao", phenotypeDao);
        inject("dosingGuidelineDao", dosingGuidelineDao);
    }

    @Test
    void matching_whenSampleIdMissing_forwardsToSamplesView() throws Exception {
        when(request.getParameter("sampleId")).thenReturn(null);
        when(request.getRequestDispatcher("/views/samples.jsp")).thenReturn(requestDispatcher);

        controller.matching(request, response);

        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void matching_whenSampleIdInvalid_redirectsToSamples() throws Exception {
        when(request.getParameter("sampleId")).thenReturn("not-a-number");

        controller.matching(request, response);

        verify(response).sendRedirect("samples");
    }

    @Test
    void matching_whenLegacyModeAndNoGenes_redirectsToSamples() throws Exception {
        when(request.getParameter("sampleId")).thenReturn("1");
        when(request.getParameter("mode")).thenReturn("legacy");
        when(annovarDao.getRefGenes(1)).thenReturn(Collections.emptyList());

        controller.matching(request, response);

        verify(response).sendRedirect("samples");
    }

    @Test
    void matching_whenLegacyModeWithMatches_setsAttributesAndForwards() throws Exception {
        when(request.getParameter("sampleId")).thenReturn("1");
        when(request.getParameter("mode")).thenReturn("legacy");
        when(annovarDao.getRefGenes(1)).thenReturn(List.of("CYP2C19"));

        DrugLabel label = new DrugLabel("id1", "name", "obj", false, false,
                "", "", "", "contains CYP2C19 gene", "", "drug");
        when(drugLabelDao.findAll()).thenReturn(List.of(label));

        Sample sample = new Sample(1, new Date(), "tester");
        when(sampleDao.findById(1)).thenReturn(sample);
        when(request.getRequestDispatcher("/views/matching_index_search.jsp")).thenReturn(requestDispatcher);

        controller.matching(request, response);

        verify(request).setAttribute(eq("matched"), any());
        verify(request).setAttribute("sample", sample);
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void uploadAnnovarOutput_whenUploadedByBlank_forwardsError() throws Exception {
        when(request.getParameter("uploaded_by")).thenReturn("  ");
        when(request.getRequestDispatcher("/views/matching_index_error.jsp")).thenReturn(requestDispatcher);

        controller.uploadAnnovarOutput(request, response);

        verify(request).setAttribute("validateError", "Uploaded by can not be blank");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void uploadAnnovarOutput_whenPartMissing_forwardsError() throws Exception {
        when(request.getParameter("uploaded_by")).thenReturn("tester");
        when(request.getPart("annovar")).thenReturn(null);
        when(request.getRequestDispatcher("/views/matching_index_error.jsp")).thenReturn(requestDispatcher);

        controller.uploadAnnovarOutput(request, response);

        verify(request).setAttribute("validateError", "annovar output file can not be blank");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void uploadAnnovarOutput_whenValidInput_savesAndRedirects() throws Exception {
        when(request.getParameter("uploaded_by")).thenReturn("tester");
        when(request.getPart("annovar")).thenReturn(part);
        InputStream inputStream = new ByteArrayInputStream("test-content".getBytes(StandardCharsets.UTF_8));
        when(part.getInputStream()).thenReturn(inputStream);
        when(sampleDao.save("tester")).thenReturn(99);

        controller.uploadAnnovarOutput(request, response);

        verify(sampleDao).save("tester");
        verify(annovarDao).save(99, "test-content");
        verify(response).sendRedirect("matching?sampleId=99");
    }

    private void inject(String fieldName, Object target) throws Exception {
        Field field = MatchingController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, target);
    }
}
