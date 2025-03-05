package be.kbc.eap.nexus.datastore.internal.group;

import be.kbc.eap.nexus.CondaPath;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.view.Content;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;

public interface CondaGroupFacet extends GroupFacet {

    /**
     * Fetches cached content if exists, or {@code null}.
     */
    @Nullable
    Content getCached(CondaPath condaPath) throws IOException;

    @FunctionalInterface
    interface ContentFunction<T>
    {
        Content apply(T data, String contentType) throws IOException;
    }

    interface MetadataMerger {
        void merge(OutputStream outputStream, CondaPath mavenPath, LinkedHashMap<Repository, Content> contents);
    }
}
