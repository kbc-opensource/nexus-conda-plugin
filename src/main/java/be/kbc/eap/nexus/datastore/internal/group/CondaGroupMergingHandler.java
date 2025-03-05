package be.kbc.eap.nexus.datastore.internal.group;

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
import org.sonatype.nexus.transaction.RetryDeniedException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Predicates.or;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class CondaGroupMergingHandler extends GroupHandler {
    private static final Predicate<Repository> PROXY_OR_GROUP =
            or(new HasFacet(ProxyFacet.class), new HasFacet(GroupFacet.class));


    @Override
    protected Response doGet(@Nonnull final Context context, @Nonnull final DispatchedRepositories dispatched) throws Exception {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        final CondaContentGroupFacetImpl groupFacet = context.getRepository().facet(CondaContentGroupFacetImpl.class);
        final Repository repository = context.getRepository();
        log.debug("Incoming request for {} : {}", context.getRepository().getName(), condaPath.getPath());

        final List<Repository> members = groupFacet.members();
        Map<Repository, Response> passThroughResponses = ImmutableMap.of();

        log.debug("groupFacet.members(): {}", members);
        // pass request through to proxies/nested-groups before checking our group cache
        Iterable<Repository> proxiesOrGroups = Iterables.filter(members, PROXY_OR_GROUP);
        if (proxiesOrGroups.iterator().hasNext()) {
            passThroughResponses = getAll(context, proxiesOrGroups, dispatched);
        }

        log.debug("groupFacet.members() to query: {}", proxiesOrGroups);
        log.debug("Pass through responses: " + passThroughResponses.size());

        if (!condaPath.isHash()) {
            log.debug("REQUEST IS NOT FOR HASH FILE");
            if (condaPath.getPath().endsWith(".zst")) {
                // ZST FILES PROCESSING
                log.debug("REQUEST IS FOR ZST FILE");

//                Optional<Content> cached = checkCache(groupFacet, condaPath, repository);
//
//                if (cached.isPresent()) {
//                    log.debug("Serving cached content {} : {}", repository.getName(), condaPath.getPath());
//                    return HttpResponses.ok(cached.get());
//                }

                Map<Repository, Response> responses = getAll(context, members, dispatched);

                for (Repository member : members) {
                    Response response = passThroughResponses.get(member);
                    log.debug("passThroughResponses.get(member): {}", response);
                    if (response == null) {
                        response = responses.get(member);
                        log.debug("passThroughResponses.get(member) == null: {}", response);
                    }
                    if (response != null) {
                        responses.put(member, response);
                        log.debug("passThroughResponses.get(member) == null: {}", response);
                    }
                }

                // merge the individual responses and cache the result
                log.debug("Zst file - Call merge and cache");
                Content content = groupFacet.mergeAndCache(condaPath, responses);
                if (content != null) {
                    log.debug("Zst file - Responses merged {} : {}", context.getRepository().getName(), condaPath.getPath());
                    return HttpResponses.ok(content);
                }
                log.debug("Zst file - Not found response to merge {} : {}", context.getRepository().getName(), condaPath.getPath());
                return HttpResponses.notFound();
            }
            else {

//                Optional<Content> cached = checkCache(groupFacet, condaPath, repository);
//
//                if (cached.isPresent()) {
//                    log.debug("Serving cached content {} : {}", repository.getName(), condaPath.getPath());
//                    return HttpResponses.ok(cached.get());
//                }

                // NON ZST FILE PROCESSING - regular JSON files
                Map<Repository, Response> allResponses = getAll(context, members, dispatched);

                log.debug("Non zst file - Remaining responses: \n" + allResponses.size());

                // merge the two sets of responses according to member order
                LinkedHashMap<Repository, Response> responses = new LinkedHashMap<>();
                for (Repository member : members) {
                    Response response = passThroughResponses.get(member);
                    if (response == null) {
                        response = allResponses.get(member);
                    }
                    if (response != null) {
                        responses.put(member, response);
                    }
                }

                // merge the individual responses and cache the result
                log.debug("Non zst file - Call merge and cache");
                Content content = groupFacet.mergeAndCache(condaPath, responses);
                if (content != null) {
                    log.debug("Non zst file - Responses merged {} : {}", context.getRepository().getName(), condaPath.getPath());
                    return HttpResponses.ok(content);
                }
                log.debug("Non zst file - Not found response to merge {} : {}", context.getRepository().getName(), condaPath.getPath());
                return HttpResponses.notFound();
            }
        } else {
            // hash should be available if corresponding content fetched. out of bound request?
            log.debug("Outbound request for hash {} : {}", context.getRepository().getName(), condaPath.getPath());
            return HttpResponses.notFound();
        }
    }

    private Optional<Content> checkCache(
            @Nonnull final CondaGroupFacet groupFacet,
            @Nonnull final CondaPath condaPath,
            @Nonnull final Repository repository)
            throws IOException
    {
        try {
            // check group-level cache to see if it's been invalidated by any updates
            return ofNullable(groupFacet.getCached(condaPath));
        }
        catch (RetryDeniedException e) {
            log.debug("Conflict fetching cached content {} : {}", repository.getName(), condaPath.getPath(), e);
        }
        return empty();
    }
}
