package be.kbc.eap.nexus.repodata;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TestRepoDataMerger {


    private InputStream loadTestFile(String fileName) throws IOException {
        Class clazz = TestRepoDataMerger.class;
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
    public void testMergeOf2RepoDataFiles() throws IOException {

        List<InputStream> streams = new ArrayList<>();
        streams.add(loadTestFile("repodata1.json"));
        streams.add(loadTestFile("repodata2.json"));

        CondaRepoDataMerger merger = new CondaRepoDataMerger();
        String jsonResult = merger.mergeRepoDataFiles(streams);

        String expectedResult = loadExpectedFile("repodata-expected.json");

        Assert.assertEquals(expectedResult, jsonResult);
    }

}
