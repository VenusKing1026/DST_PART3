package cn.edu.zju.controller;

import cn.edu.zju.bean.Sample;
import cn.edu.zju.dao.SampleDao;
import org.junit.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MatchingControllerErrorHandlingTest {

    @Test
    public void uploadRejectsMissingInputType() throws Exception {
        RequestState state = new RequestState();
        state.parameters.put("uploaded_by", "tester");
        state.part = part("sample.vcf", "vcf-content");

        new MatchingController().uploadVariantFile(request(state), response(state));

        assertEquals("/views/matching_index_error.jsp", state.forwardedPath);
        assertEquals("Input type is required.", state.attributes.get("error"));
    }

    @Test
    public void uploadRejectsMissingUploader() throws Exception {
        RequestState state = new RequestState();
        state.parameters.put("input_type", "vcf");
        state.part = part("sample.vcf", "vcf-content");

        new MatchingController().uploadVariantFile(request(state), response(state));

        assertEquals("/views/matching_index_error.jsp", state.forwardedPath);
        assertEquals("Uploaded by is required.", state.attributes.get("error"));
    }

    @Test
    public void uploadRejectsEmptyFile() throws Exception {
        RequestState state = new RequestState();
        state.parameters.put("input_type", "vcf");
        state.parameters.put("uploaded_by", "tester");
        state.part = part("empty.vcf", "");

        new MatchingController().uploadVariantFile(request(state), response(state));

        assertEquals("/views/matching_index_error.jsp", state.forwardedPath);
        assertEquals("Please select a file.", state.attributes.get("error"));
    }

    @Test
    public void uploadRejectsUnsupportedInputType() throws Exception {
        RequestState state = new RequestState();
        state.parameters.put("input_type", "pdf");
        state.parameters.put("uploaded_by", "tester");
        state.part = part("sample.pdf", "not a variant file");

        MatchingController controller = new MatchingController();
        setSampleDao(controller, new InMemorySampleDao());
        controller.uploadVariantFile(request(state), response(state));

        assertEquals("/views/matching_index_error.jsp", state.forwardedPath);
        assertEquals("Unsupported input type: pdf", state.attributes.get("error"));
    }

    private HttpServletRequest request(RequestState state) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getParameter".equals(name)) {
                        return state.parameters.get((String) args[0]);
                    }
                    if ("getPart".equals(name)) {
                        return state.part;
                    }
                    if ("setAttribute".equals(name)) {
                        state.attributes.put((String) args[0], args[1]);
                        return null;
                    }
                    if ("getRequestDispatcher".equals(name)) {
                        String path = (String) args[0];
                        return dispatcher(state, path);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private HttpServletResponse response(RequestState state) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("sendRedirect".equals(method.getName())) {
                        state.redirectedPath = (String) args[0];
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private RequestDispatcher dispatcher(RequestState state, String path) {
        return (RequestDispatcher) Proxy.newProxyInstance(
                RequestDispatcher.class.getClassLoader(),
                new Class[]{RequestDispatcher.class},
                (proxy, method, args) -> {
                    if ("forward".equals(method.getName())) {
                        state.forwardedPath = path;
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Part part(String fileName, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return (Part) Proxy.newProxyInstance(
                Part.class.getClassLoader(),
                new Class[]{Part.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getSubmittedFileName".equals(name)) {
                        return fileName;
                    }
                    if ("getSize".equals(name)) {
                        return (long) bytes.length;
                    }
                    if ("getInputStream".equals(name)) {
                        return new ByteArrayInputStream(bytes);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == void.class) {
            return null;
        }
        return 0;
    }

    private void setSampleDao(MatchingController controller, SampleDao sampleDao) throws Exception {
        Field field = MatchingController.class.getDeclaredField("sampleDao");
        field.setAccessible(true);
        field.set(controller, sampleDao);
    }

    private static class RequestState {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private Part part;
        private String forwardedPath;
        private String redirectedPath;
    }

    private static class InMemorySampleDao extends SampleDao {
        @Override
        public int save(Sample sample) {
            return 300;
        }

        @Override
        public void updateParseStatus(int id, String parseStatus) {
        }
    }
}
