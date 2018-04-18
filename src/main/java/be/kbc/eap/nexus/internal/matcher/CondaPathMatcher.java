package be.kbc.eap.nexus.internal.matcher;

import be.kbc.eap.nexus.CondaPathParser;

public class CondaPathMatcher extends CondaMatcherSupport
{
    public CondaPathMatcher(final CondaPathParser mavenPathParser) {
        super(mavenPathParser, (String path) -> Boolean.TRUE);
    }
}
