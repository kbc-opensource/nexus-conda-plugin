package be.kbc.eap.nexus.util;

import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;

import static org.junit.Assert.assertEquals;

public class CondaPathUtilsTest extends TestSupport {



    @Test
    public void doesPrependPathTest() {

        String result = CondaPathUtils.normalizeAssetPath("path");
        assertEquals("/path", result);

    }

}
