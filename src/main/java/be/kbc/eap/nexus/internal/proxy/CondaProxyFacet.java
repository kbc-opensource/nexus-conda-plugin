package be.kbc.eap.nexus.internal.proxy;

import be.kbc.eap.nexus.CondaFacet;
import be.kbc.eap.nexus.CondaPath;
import com.google.common.io.Closeables;
import org.apache.http.StatusLine;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.proxy.ProxyServiceException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import java.io.IOException;
import java.io.UncheckedIOException;

@Named
@Facet.Exposed
public class CondaProxyFacet
extends ProxyFacetSupport {
    @Nullable
    @Override
    protected Content getCachedContent(Context context) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.info("Retrieve " + condaPath.getPath() + " from storage");
        return getCondaFacet().get(condaPath);
    }


    @Override
    protected Content doGet(Context context, @Nullable Content staleContent) throws IOException {
        Content remote = null;
        Object content = staleContent;



        try {
            log.info("Fetch " + context.getRequest().getPath() + " from remote ");
            remote = this.fetch(context, (Content)content);
            log.info("Fetched from remote");
            if (remote != null) {
                log.info("Store content locally");
                content = this.store(context, remote);
//                if (this.contentCooperation != null && remote.equals(content)) {
//                    content = new TempContent(remote);
//                }
            }
        } catch (ProxyServiceException var11) {
            this.logContentOrThrow((Content)content, context, var11.getHttpResponse().getStatusLine(), var11);
        } catch (IOException var12) {
            this.logContentOrThrow((Content)content, context, (StatusLine)null, var12);
        } catch (UncheckedIOException var13) {
            this.logContentOrThrow((Content)content, context, (StatusLine)null, var13.getCause());
        } finally {
            if (remote != null && !remote.equals(content)) {
                Closeables.close(remote, true);
            }

        }

        log.info("Return Content");
        return (Content)content;
    }

    <X extends Throwable> String buildLogContentMessage(@Nullable Content content, @Nullable StatusLine statusLine) {
        StringBuilder message = new StringBuilder("Exception {} checking remote for update");
        if (statusLine == null) {
            message.append(", proxy repo {} failed to fetch {}");
        } else {
            message.append(", proxy repo {} failed to fetch {} with status line {}");
        }

        if (content == null) {
            message.append(", content not in cache.");
        } else {
            message.append(", returning content from cache.");
        }

        return message.toString();
    }

    private <X extends Throwable> void logContentOrThrow(@Nullable Content content, Context context, @Nullable StatusLine statusLine, X exception) throws X {
        String logMessage = this.buildLogContentMessage(content, statusLine);
        String repositoryName = context.getRepository().getName();
        String contextUrl = this.getUrl(context);
        if (content != null) {
            this.log.debug(logMessage, new Object[]{exception, repositoryName, contextUrl, statusLine});
        } else {
            if (this.log.isDebugEnabled()) {
                this.log.warn(logMessage, new Object[]{exception, repositoryName, contextUrl, statusLine, exception});
            } else {
                this.log.warn(logMessage, new Object[]{exception, repositoryName, contextUrl, statusLine});
            }

            throw exception;
        }
    }

    @Override
    protected Content store(Context context, Content content) throws IOException {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        log.info("Store content for path " + condaPath.getPath());
        return getCondaFacet().put(condaPath, content);
    }

    @Override
    protected void indicateVerified(Context context, Content content, CacheInfo cacheInfo) throws IOException {
        //
    }

    @Override
    protected String getUrl(@Nonnull Context context) {
        final CondaPath condaPath = context.getAttributes().require(CondaPath.class);
        return condaPath.getPath();
    }

    private CondaFacet getCondaFacet() {
        return getRepository().facet(CondaFacet.class);
    }


}
