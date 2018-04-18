package be.kbc.eap.nexus.internal;

import org.sonatype.nexus.repository.view.ContentTypes;

public class Constants {

    public static final String REPODATA_JSON = "repodata.json";

    /**
     * Content Type of Maven2 checksum files (sha1, md5).
     */
    public static final String CHECKSUM_CONTENT_TYPE = ContentTypes.TEXT_PLAIN;

}
