package be.kbc.eap.nexus.util;

import be.kbc.eap.nexus.CondaPath;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestCondaPathParser {


    private final CondaPathParserImpl parser = new CondaPathParserImpl();

    @Test
    public void testCondaPathToCoordinates_withValidPath() {
        String path = "osx-64/abseil-cpp-20200225.2-h23ab428_0.conda";
        CondaPath.Coordinates coordinates = parser.condaPathToCoordinates(path, true);
        assertNotNull(coordinates);
        assertEquals("osx-64", coordinates.getNamespace());
        assertEquals("abseil-cpp", coordinates.getPackageName());
        assertEquals("20200225.2", coordinates.getVersion());
        assertEquals("h23ab428_0", coordinates.getBuildString());
        assertEquals("conda", coordinates.getExtension());
    }

    @Test
    public void testCondaPathToCoordinates_withRepodataJson() {
        String path = "osx-64/repodata.json";
        CondaPath.Coordinates coordinates = parser.condaPathToCoordinates(path, true);
        assertNull(coordinates);
    }

    @Test
    public void testCondaPathToCoordinates_withInvalidPath() {
        String path = "invalidpath";
        CondaPath.Coordinates coordinates = parser.condaPathToCoordinates(path, true);
        assertNull(coordinates);
    }

    @Test
    public void testCondaPathToCoordinates_withNoExtension() {
        String path = "osx-64/package-1.0.0-0";
        CondaPath.Coordinates coordinates = parser.condaPathToCoordinates(path, true);
        assertNotNull(coordinates);
        assertEquals("osx-64", coordinates.getNamespace());
        assertEquals("package", coordinates.getPackageName());
        assertEquals("1.0.0", coordinates.getVersion());
        assertEquals("0", coordinates.getBuildString());
        assertEquals("", coordinates.getExtension());
    }

}
