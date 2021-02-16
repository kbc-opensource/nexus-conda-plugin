package be.kbc.eap.nexus.internal;

import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.WritePolicySelector;

public class CondaWritePolicySelector implements WritePolicySelector {


    @Override
    public WritePolicy select(Asset asset, WritePolicy configured) {
        if (WritePolicy.ALLOW_ONCE == configured) {
            String name = asset.name();
            if (name.endsWith(".tar.bz2")) {
                return WritePolicy.ALLOW_ONCE;
            }
            else {
                return WritePolicy.ALLOW;
            }
        }
        return configured;
    }
}
