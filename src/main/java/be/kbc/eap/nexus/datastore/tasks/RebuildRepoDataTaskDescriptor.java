package be.kbc.eap.nexus.datastore.tasks;

import be.kbc.eap.nexus.CondaFormat;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.internal.datastore.DatabaseCheckImpl;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
@AvailabilityVersion(from = "1.0")
public class RebuildRepoDataTaskDescriptor
        extends TaskDescriptorSupport {


    public static final String TYPE_ID = "repository.conda.rebuild-repodata";

    public static final String REPOSITORY_NAME_FIELD_ID = "repositoryName";

    public RebuildRepoDataTaskDescriptor() {
        super(TYPE_ID,
                RebuildRepoDataTask.class,
                "Conda - Rebuild repodata.json",
                VISIBLE,
                EXPOSED,
                new RepositoryCombobox(
                        REPOSITORY_NAME_FIELD_ID,
                        "Repository",
                        "Select the hosted conda repository to rebuild repodata",
                        true
                ).includingAnyOfFormats(CondaFormat.NAME).includingAnyOfTypes(HostedType.NAME)
                        .includeAnEntryForAllRepositories());
    }
}
