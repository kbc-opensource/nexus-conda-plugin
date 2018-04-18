package be.kbc.eap.nexus.internal.matcher;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.internal.Constants;

public class CondaRepoDataMatcher
    extends CondaMatcherSupport {


    private static final String CONDA_REPODATA_REQ_PATH = "/" + Constants.REPODATA_JSON;

    public CondaRepoDataMatcher(final CondaPathParser mavenPathParser) {
        super(mavenPathParser,
                withHashes((String path) -> path.endsWith(CONDA_REPODATA_REQ_PATH))
        );
    }
}
