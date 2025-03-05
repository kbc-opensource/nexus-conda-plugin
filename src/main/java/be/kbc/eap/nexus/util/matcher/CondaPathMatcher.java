package be.kbc.eap.nexus.util.matcher;

import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.util.CondaMatcherSupport;

public class CondaPathMatcher extends CondaMatcherSupport
{
    public CondaPathMatcher(final CondaPathParser mavenPathParser) {
        super(mavenPathParser, (String path) -> Boolean.TRUE);
    }
}
