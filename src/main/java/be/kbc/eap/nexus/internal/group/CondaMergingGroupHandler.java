package be.kbc.eap.nexus.internal.group;

import be.kbc.eap.nexus.CondaPath;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.sonatype.nexus.repository.HasFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Predicates.or;

@Singleton
@Named
public class CondaMergingGroupHandler
        extends GroupHandler
{
    private static final Predicate<Repository> PROXY_OR_GROUP =
            or(new HasFacet(ProxyFacet.class), new HasFacet(GroupFacet.class));


    @Override
    protected Response doGet(@Nonnull final Context context,
                             @Nonnull final DispatchedRepositories dispatched) throws Exception
    {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        final CondaGroupFacet groupFacet = context.getRepository().facet(CondaGroupFacet.class);
        log.info("Incoming request for {} : {}", context.getRepository().getName(), condaPath.getPath());

        final List<Repository> members = groupFacet.members();

        Map<Repository, Response> passThroughResponses = ImmutableMap.of();

        // pass request through to proxies/nested-groups before checking our group cache
        Iterable<Repository> proxiesOrGroups = Iterables.filter(members, PROXY_OR_GROUP);
        if (proxiesOrGroups.iterator().hasNext()) {
            passThroughResponses = getAll(context, proxiesOrGroups, dispatched);
        }

        log.info("Pass through responses: " + passThroughResponses.size());

        // now check group-level cache to see if it's been invalidated by any updates
        Content content = groupFacet.getCached(condaPath);
        if (content != null) {
            log.info("Serving cached content {} : {}", context.getRepository().getName(), condaPath.getPath());
            return HttpResponses.ok(content);
        }

        if (!condaPath.isHash()) {
            // this will fetch the remaining responses, thanks to the 'dispatched' tracking
            Map<Repository, Response> remainingResponses = getAll(context, members, dispatched);

            log.info("Remaining responses: " + remainingResponses.size());
            // merge the two sets of responses according to member order
            LinkedHashMap<Repository, Response> responses = new LinkedHashMap<>();
            for (Repository member : members) {
                Response response = passThroughResponses.get(member);
                if (response == null) {
                    response = remainingResponses.get(member);
                }
                if (response != null) {
                    responses.put(member, response);
                }
            }

            // merge the individual responses and cache the result
            log.info("Call merge and cache");
            content = groupFacet.mergeAndCache(condaPath, responses);
            if (content != null) {
                log.info("Responses merged {} : {}", context.getRepository().getName(), condaPath.getPath());
                return HttpResponses.ok(content);
            }
            log.info("Not found respone to merge {} : {}", context.getRepository().getName(), condaPath.getPath());
            return HttpResponses.notFound();
        }
        else {
            // hash should be available if corresponding content fetched. out of bound request?
            log.info("Outbound request for hash {} : {}", context.getRepository().getName(), condaPath.getPath());
            return HttpResponses.notFound();
        }
    }


}
