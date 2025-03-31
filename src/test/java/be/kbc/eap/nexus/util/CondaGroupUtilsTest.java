package be.kbc.eap.nexus.util;

import be.kbc.eap.nexus.datastore.internal.CondaGroupUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CondaGroupUtilsTest extends TestSupport {

    @Mock
    private Repository groupRepository;

    @Mock
    private Response response1;

    @Mock
    private Response response2;

    @Mock
    private Repository repository1;

    @Mock
    private Repository repository2;

    @Mock
    private Payload payload1;

    @Mock
    private Payload payload2;

    @Mock
    private Logger logger;

    @Mock
    private Status status;

    private InputStream loadTestFile(String fileName) throws IOException {
        Class clazz = CondaGroupUtilsTest.class;
        URL resource = clazz.getResource(fileName);
        return resource.openStream();
    }

    private String loadExpectedFile(String fileName) throws IOException {
        InputStream expected = loadTestFile(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(expected));
        StringBuffer result = new StringBuffer();
        String line = null;
        while((line = br.readLine()) != null)
        {
            result.append(line + "\n");
        }

        return result.toString().trim();
    }

    @Test
    public void doesMergeOfRepodataJson() throws IOException {

        Map<Repository, Response> responseMap = Maps.newLinkedHashMap();

        responseMap.put(repository1, response1);
        responseMap.put(repository2, response2);

        when(status.getCode()).thenReturn(200);

        when(response1.getStatus()).thenReturn(status);
        when(response2.getStatus()).thenReturn(status);

        when(response1.getPayload()).thenReturn(payload1);
        when(response2.getPayload()).thenReturn(payload2);

        when(payload1.openInputStream()).thenReturn(loadTestFile("repodata1.json"));
        when(payload2.openInputStream()).thenReturn(loadTestFile("repodata2.json"));



        Supplier<String> result = CondaGroupUtils.lazyMergeResult(groupRepository, responseMap, false, logger);


        String expected = prettyPrintJson(loadExpectedFile("repodata-expected.json"));



        assertJonsEqual(expected, prettyPrintJson(result.get()));

    }

    @Test
    public void doesMergeOfRepodataJsonZst() throws IOException {

        Map<Repository, Response> responseMap = Maps.newLinkedHashMap();

        responseMap.put(repository1, response1);
        responseMap.put(repository2, response2);

        when(status.getCode()).thenReturn(200);

        when(response1.getStatus()).thenReturn(status);
        when(response2.getStatus()).thenReturn(status);

        when(response1.getPayload()).thenReturn(payload1);
        when(response2.getPayload()).thenReturn(payload2);

        when(payload1.openInputStream()).thenReturn(loadTestFile("repodata1.json.zst"));
        when(payload2.openInputStream()).thenReturn(loadTestFile("repodata2.json.zst"));



        Supplier<String> result = CondaGroupUtils.lazyMergeResult(groupRepository, responseMap, true, logger);


        String expected = prettyPrintJson(loadExpectedFile("repodata-expected.json"));



        assertJonsEqual(expected, prettyPrintJson(result.get()));

    }

    private String prettyPrintJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(json);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }


    private void assertJonsEqual(String expected, String actual) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectNode expectedRoot = (ObjectNode) mapper.readTree(expected);
            ObjectNode actualRoot = (ObjectNode) mapper.readTree(actual);

            Boolean isEqual = expectedRoot.equals(actualRoot);
            assertTrue(isEqual);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
