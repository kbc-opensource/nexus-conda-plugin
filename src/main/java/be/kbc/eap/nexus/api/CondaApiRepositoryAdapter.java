package be.kbc.eap.nexus.api;

import javax.inject.Inject;
import javax.inject.Named;

import be.kbc.eap.nexus.CondaFormat;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;


/**
 * Adapter to expose Conda specific properties for the repositories REST API.
 * This is currently not used because there are no Conda specific properties defined.
 * The implementation of this class is practically identical to the "SimpleApiRepositoryAdapter" it extends.
 *
 * Look at "CondaHostedRepositoriesAttributes" to see an example on how to define custom properties.
 */
@Named(CondaFormat.NAME)
public class CondaApiRepositoryAdapter
        extends SimpleApiRepositoryAdapter {
    @Inject
    public CondaApiRepositoryAdapter(final RoutingRuleStore routingRuleStore) {
        super(routingRuleStore);
    }

    @Override
    public AbstractApiRepository adapt(final Repository repository) {
        boolean online = repository.getConfiguration().isOnline();
        String name = repository.getName();
        String url = repository.getUrl();

        switch (repository.getType().toString()) {
            case HostedType.NAME:
                return new CondaHostedApiRepository(
                        name,
                        url,
                        online,
                        getHostedStorageAttributes(repository),
                        getCleanupPolicyAttributes(repository),
                        createCondaHostedRepositoriesAttributes(repository),
                        getComponentAttributes(repository)
                );
            case ProxyType.NAME:
                return new CondaProxyApiRepository(name, url, online,
                        getHostedStorageAttributes(repository),
                        getCleanupPolicyAttributes(repository),
                        createCondaProxyRepositoriesAttributes(repository),
                        getProxyAttributes(repository),
                        getNegativeCacheAttributes(repository),
                        getHttpClientAttributes(repository),
                        getRoutingRuleName(repository),
                        getReplicationAttributes(repository));

            case GroupType.NAME:
                return new CondaGroupApiRepository(
                        name,
                        url,
                        online,
                        getHostedStorageAttributes(repository),
                        createCondaGroupRepositoriesAttributes(repository),
                        getGroupAttributes(repository)
                );
        }
        return null;
    }

    private CondaHostedRepositoriesAttributes createCondaHostedRepositoriesAttributes(final Repository repository) {
//         No extra attributes, so creates empty class, this is an example from APT plugin
//         String distribution = repository.getConfiguration().attributes(Apt.NAME).get("distribution", String.class);
        return new CondaHostedRepositoriesAttributes();
    }

    private CondaProxyRepositoriesAttributes createCondaProxyRepositoriesAttributes(final Repository repository) {
//         Optional if there are extra attributes to be imported
//         NestedAttributesMap condaAttributes = repository.getConfiguration().attributes(CondaFormat.NAME);
//         String distribution = condaAttributes.get("distribution", String.class);
//         Boolean flat = condaAttributes.get("flat", Boolean.class);
        return new CondaProxyRepositoriesAttributes();
    }

    private CondaGroupRepositoriesAttributes createCondaGroupRepositoriesAttributes(final Repository repository) {
//        optional method, only if there are extra attributes to be imported
        return new CondaGroupRepositoriesAttributes();
    }
}
