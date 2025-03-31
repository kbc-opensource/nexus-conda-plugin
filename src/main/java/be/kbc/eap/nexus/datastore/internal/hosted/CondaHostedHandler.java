package be.kbc.eap.nexus.datastore.internal.hosted;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.datastore.CondaContentFacet;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

import static org.sonatype.nexus.repository.http.HttpMethods.*;

@Named
@Singleton
public class CondaHostedHandler extends ComponentSupport implements Handler {

    @Nonnull
    @Override
    public Response handle(@Nonnull Context context) throws Exception {
        String method = context.getRequest().getAction();
        CondaPath path = context.getAttributes().require(CondaPath.class);

        log.debug("Handling {} request in {} for path: {}", context.getRepository().getName(), method, path.getPath());

        CondaContentFacet condaFacet = context.getRepository().facet(CondaContentFacet.class);
        switch (method) {
            case GET:
            case HEAD:
                return doGet(path, condaFacet);
            case PUT:
                return doPut(context, path, condaFacet);
            case DELETE:
                return doDelete(path, condaFacet, context.getRepository());
            default:
                return HttpResponses.methodNotAllowed(context.getRequest().getAction(), GET, HEAD, PUT, DELETE);

        }
    }

    private Response doGet(CondaPath path, CondaContentFacet condaFacet) throws IOException {

        Optional<FluentAsset> content = condaFacet.getAsset(path.getPath());
        if (!content.isPresent()) {
            return HttpResponses.notFound(path.getPath());
        }
        return HttpResponses.ok(content.get().download());
    }

    private Response doPut(@Nonnull final Context context, final CondaPath path, final CondaContentFacet condaFacet) throws IOException {
        condaFacet.put(path, context.getRequest().getPayload());
        return HttpResponses.created();
    }

    private Response doDelete(final CondaPath condaPath, final CondaContentFacet condaFacet, final Repository repository) throws IOException {
        log.info("Delete " + condaPath.getPath());
        boolean deleted = condaFacet.delete(condaPath);
        if(!deleted) {
            return HttpResponses.notFound(condaPath.getPath());
        }
        condaFacet.rebuildRepoDataJson(repository);
        return HttpResponses.noContent();
    }
}
