package be.kbc.eap.nexus.internal;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class CondaFacetImplTest {

    private CondaFacetImpl facet;
    private CondaPathParser parser;
    private Gson gson;

    @Before
    public void setUp() throws Exception {
        final Map<String, CondaPathParser> condaPathParserMap = Collections.emptyMap();
        facet = new CondaFacetImpl(condaPathParserMap);
        parser = new CondaPathParserImpl();
        gson = new Gson();
    }


    @Test(expected = NullPointerException.class)
    public void testPut() throws IOException {
        CondaPath path = this.parser.parsePath("noarch/test_prj-0.0.1.dev-py27_20190611100529.tar.bz2");
        Payload content = new StringPayload("some content", "application/x-tar");
        String indexJson = gson.toJson(new IndexJson());

        facet.put(path, content, indexJson);
    }

}

class IndexJson {
    long size = 3772;
    String subdir = "noarch";
    long build_number = 20190611100529L;
    String name = "test_prj";
    String license = "WTFPL";
    long timestamp = 1560247671929L;
    String noarch = "generic";
    String[] depends = { "python >=2.7,<2.8.0a0" };
    String version = "0.0.1.dev";
    String build = "py27_20190611100529";
    String sha256 = "1f36c3ef83988b29e5c8dbeb9751cf3a55fbd4353c803c6b3b8dae79a019669e";
    String md5 = "5ce9195417e11bed705b23f4a7008b62";
}