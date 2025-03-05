package be.kbc.eap.nexus.datastore.tasks;

import be.kbc.eap.nexus.CondaFormat;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.transaction.UnitOfWork;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class RebuildRepoDataTask
    extends RepositoryTaskSupport
    implements Cancelable {

    private final Type hostedType;

    private final Format condaFormat;

    @Inject
    public RebuildRepoDataTask(@Named(HostedType.NAME) final Type hostedType,
                                     @Named(CondaFormat.NAME) final Format maven2Format)
    {
        this.hostedType = checkNotNull(hostedType);
        this.condaFormat = checkNotNull(maven2Format);
    }


    @Override
    protected void execute(final Repository repository) {
        CondaContentFacet condaFacet = repository.facet(CondaContentFacet.class);
        try {
            condaFacet.rebuildRepoDataJson();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean appliesTo(final Repository repository) {
        return condaFormat.equals(repository.getFormat()) && hostedType.equals(repository.getType());
    }

    @Override
    public String getMessage() {
        return "Rebuilding Conda Repodata.json of " + getRepositoryField();
    }
}
