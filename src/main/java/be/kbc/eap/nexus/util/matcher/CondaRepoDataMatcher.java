package be.kbc.eap.nexus.util.matcher;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.util.CondaMatcherSupport;
import be.kbc.eap.nexus.util.Constants;

public class CondaRepoDataMatcher extends CondaMatcherSupport {

    private static final String CONDA_REPODATA_REQ_PATH = "/" + Constants.REPODATA_JSON;
    private static final String CONDA_ZST_REPODATA_REQ_PATH = "/" + Constants.REPODATA_JSON_ZST;

    public CondaRepoDataMatcher(final CondaPathParser mavenPathParser) {
        super(mavenPathParser, withHashes((String path) -> (path.endsWith(CONDA_REPODATA_REQ_PATH) || path.endsWith(CONDA_ZST_REPODATA_REQ_PATH))));
    }
}
