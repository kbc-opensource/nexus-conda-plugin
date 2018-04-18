package be.kbc.eap.nexus;

public class CondaCoordinatesHelper
{
    public static String getGroup(String path) {
        StringBuilder group = new StringBuilder();
        if (!path.startsWith("/")) {
            group.append("/");
        }
        int i = path.lastIndexOf("/");
        if (i != -1) {
            group.append(path.substring(0, i));
        }
        return group.toString();
    }

    private CondaCoordinatesHelper() {
        // Don't instantiate
    }
}
