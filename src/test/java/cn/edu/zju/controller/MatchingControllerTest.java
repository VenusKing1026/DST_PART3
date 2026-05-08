package cn.edu.zju.controller;

import cn.edu.zju.bean.DrugLabel;
import cn.edu.zju.bean.Sample;
import cn.edu.zju.bean.User;
import cn.edu.zju.dao.AnnovarDao;
import cn.edu.zju.dao.DosingGuidelineDao;
import cn.edu.zju.dao.DrugLabelDao;
import cn.edu.zju.dao.GenotypeDao;
import cn.edu.zju.dao.MatchingResultDao;
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
import javax.servlet.http.HttpSession;
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
import static org.mockito.Mockito.lenient;
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
    private HttpSession session;

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
    @Mock
    private MatchingResultDao matchingResultDao;

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
        inject("matchingResultDao", matchingResultDao);

        User mockUser = new User();
        mockUser.setId(1);
        lenient().when(request.getSession(false)).thenReturn(session);
        lenient().when(session.getAttribute("currentUser")).thenReturn(mockUser);
        lenient().when(request.getContextPath()).thenReturn("");
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

        Sample sample = new Sample(1, 1, new Date(), "tester", "annovar", "test.txt", "finished");
        when(sampleDao.findById(1)).thenReturn(sample);
        when(request.getRequestDispatcher("/views/matching_index_search.jsp")).thenReturn(requestDispatcher);

        controller.matching(request, response);

        verify(request).setAttribute(eq("matched"), any());
        verify(request).setAttribute("sample", sample);
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void uploadVariantFile_whenInputTypeMissing_forwardsError() throws Exception {
        when(request.getParameter("input_type")).thenReturn("");
        when(request.getRequestDispatcher("/views/matching_index_error.jsp")).thenReturn(requestDispatcher);

        controller.uploadVariantFile(request, response);

        verify(request).setAttribute("error", "Input type is required.");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void uploadVariantFile_whenUploadedByMissing_forwardsError() throws Exception {
        when(request.getParameter("input_type")).thenReturn("annovar");
        when(request.getParameter("uploaded_by")).thenReturn("");
        when(request.getRequestDispatcher("/views/matching_index_error.jsp")).thenReturn(requestDispatcher);

        controller.uploadVariantFile(request, response);

        verify(request).setAttribute("error", "Uploaded by is required.");
        verify(requestDispatcher).forward(request, response);
    }

    @Test
    void uploadVariantFile_whenPartMissing_forwardsError() throws Exception {
        when(request.getParameter("input_type")).thenReturn("annovar");
        when(request.getParameter("uploaded_by")).thenReturn("tester");
        when(request.getPart("variant_file")).thenReturn(null);
        when(request.getRequestDispatcher("/views/matching_index_error.jsp")).thenReturn(requestDispatcher);

        controller.uploadVariantFile(request, response);

        verify(request).setAttribute("error", "Please select a file.");
        verify(requestDispatcher).forward(request, response);
    }

    private void inject(String fieldName, Object target) throws Exception {
        Field field = MatchingController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, target);
    }
}
