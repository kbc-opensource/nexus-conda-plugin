package be.kbc.eap.nexus.internal.hosted;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;

import javax.inject.Named;

@Named
@Facet.Exposed
public class CondaHostedFacet extends FacetSupport {

}
