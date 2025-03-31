package be.kbc.eap.nexus.datastore.internal.hosted;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.CondaPathParser;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

@Named
@Facet.Exposed
public class CondaHostedFacetImpl extends FacetSupport implements EventAware.Asynchronous {

    @Subscribe
    @AllowConcurrentEvents
    public void onAssetDeletedEvent(final AssetDeletedEvent event) {

        //log.debug("Asset deleted event");
        event.getRepository().ifPresent(repository -> {

            if(repository.getFormat().getValue().equalsIgnoreCase(CondaFormat.NAME)) {
                log.debug("Asset deleted event for repository {}", repository.getName());
                CondaContentFacet condaContentFacet = repository.facet(CondaContentFacet.class);
                try {
                    condaContentFacet.rebuildRepoDataJson(repository);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }
}
