package be.kbc.eap.nexus.internal.hosted;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import javax.annotation.Nonnull;

import java.io.IOException;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;

public class CondaHostedHandler extends ComponentSupport implements Handler {

    @Nonnull
    @Override
    public Response handle(@Nonnull Context context) throws Exception {
        String method = context.getRequest().getAction();
        CondaPath path = context.getAttributes().require(CondaPath.class);
        CondaFacet condaFacet = context.getRepository().facet(CondaFacet.class);
        switch (method) {
            case GET:
            case HEAD:
                return doGet(path, condaFacet);
            case PUT:
                return doPut(context, path, condaFacet);
            case DELETE:
                return doDelete(path, condaFacet);
            default:
                return HttpResponses.methodNotAllowed(context.getRequest().getAction(), GET, HEAD, PUT, DELETE);

        }
    }

    private Response doGet(CondaPath path, CondaFacet condaFacet) throws IOException {

        Content content = condaFacet.get(path);
        if (content == null) {
            return HttpResponses.notFound(path.getPath());
        }
        return HttpResponses.ok(content);
    }

    private Response doPut(@Nonnull final Context context, final CondaPath path, final CondaFacet condaFacet) throws IOException {
        condaFacet.put(path, context.getRequest().getPayload());
        return HttpResponses.created();
    }

    private Response doDelete(final CondaPath condaPath, final CondaFacet condaFacet) throws IOException {
        log.info("Delete " + condaPath.getPath());
        boolean deleted = condaFacet.delete(condaPath);
        if(!deleted) {
            return HttpResponses.notFound(condaPath.getPath());
        }
        condaFacet.rebuildRepoDataJson();
        return HttpResponses.noContent();
    }
}
