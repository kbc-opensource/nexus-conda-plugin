package be.kbc.eap.nexus.internal.hosted;

import be.kbc.eap.nexus.CondaFacet;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.transaction.Transactional;

import javax.inject.Named;
import java.io.IOException;

@Named
public class CondaHostedComponentMaintenanceFacet
    extends DefaultComponentMaintenanceImpl {

    @Override
    @TransactionalDeleteBlob
    protected void deleteComponentTx(EntityId componentId, boolean deleteBlobs) {
        super.deleteComponentTx(componentId, deleteBlobs);

        afterDelete();

    }

    @Override
    @TransactionalDeleteBlob
    protected void deleteAssetTx(EntityId assetId, boolean deleteBlob) {
        super.deleteAssetTx(assetId, deleteBlob);
        afterDelete();
    }


    private void afterDelete() {
        try {
            getRepository().facet(CondaFacet.class).rebuildRepoDataJson();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
