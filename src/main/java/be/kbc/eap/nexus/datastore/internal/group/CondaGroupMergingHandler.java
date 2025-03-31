package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.datastore.internal.CondaGroupUtils;
import be.kbc.eap.nexus.util.Constants;
import com.google.common.annotations.VisibleForTesting;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CondaGroupMergingHandler extends GroupHandler {

    @Override
//    @VisibleForTesting
    protected Response doGet(@Nonnull final Context context, @Nonnull final DispatchedRepositories dispatched) throws Exception {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);

        if (!condaPath.isHash()) {
            if (condaPath.getPath().endsWith(Constants.REPODATA_JSON_ZST)) {
                return this.respond(this.getRepodataJsonZst(context));
            } else if (condaPath.getPath().endsWith(Constants.REPODATA_JSON)) {
                return this.respond(this.getRepodataJson(context));
            } else {
                return HttpResponses.badRequest();
            }
        } else {
            return HttpResponses.notFound();
        }
    }

    @Nullable
    private Content getRepodataJson(final Context context) throws Exception {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.debug("Fetching repodata.json in {} for {}", context.getRepository().getName(), condaPath.getPath());
        final CondaContentGroupFacet groupFacet = group(context);
        // we ignore the dispatched repositories, as these can contain repositories that are already fetched in a parent group, meaning they would not be included in the merge
        final Map<Repository, Response> memberResponses = this.getAll(context, groupFacet.members(), new DispatchedRepositories());
        return groupFacet.getRepodataFile(context.getRepository(), condaPath).filter(cached -> !groupFacet.isStale(cached, memberResponses)).orElseGet(() -> groupFacet.buildRepodataJson(context.getRepository(), condaPath, CondaGroupUtils.lazyMergeResult(context.getRepository(), memberResponses, false, log)));
    }

    @Nullable
    private Content getRepodataJsonZst(final Context context) throws Exception {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.debug("Fetching repodata.zst in {} for {}", context.getRepository().getName(), condaPath.getPath());
        final CondaContentGroupFacet groupFacet = group(context);
        // we ignore the dispatched repositories, as these can contain repositories that are already fetched in a parent group, meaning they would not be included in the merge
        final Map<Repository, Response> memberResponses = this.getAll(context, groupFacet.members(), new DispatchedRepositories());
        return groupFacet.getRepodataFile(context.getRepository(), condaPath).filter(cached -> !groupFacet.isStale(cached, memberResponses)).orElseGet(() -> groupFacet.buildRepodataZst(context.getRepository(), condaPath, CondaGroupUtils.lazyMergeResult(context.getRepository(), memberResponses, true, log)));
    }

    private Response respond(@Nullable final Content content) {
        return (content != null) ? HttpResponses.ok((Payload)content) : HttpResponses.notFound();
    }

    private CondaContentGroupFacet group(final Context context) {
        return context.getRepository().facet(CondaContentGroupFacet.class);
    }
}
