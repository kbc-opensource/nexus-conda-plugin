package be.kbc.eap.nexus.internal.hosted;

import be.kbc.eap.nexus.CondaFacet;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.transaction.Transactional;

import javax.inject.Named;
import java.io.IOException;
import java.util.Set;

@Named
public class CondaHostedComponentMaintenanceFacet
    extends DefaultComponentMaintenanceImpl {

    @Override
    @TransactionalDeleteBlob
    protected DeletionResult deleteComponentTx(EntityId componentId, boolean deleteBlobs) {
        DeletionResult deletionResult = super.deleteComponentTx(componentId, deleteBlobs);
        afterDelete();
        return deletionResult;
    }

    @Override
    @TransactionalDeleteBlob
    protected Set<String> deleteAssetTx(EntityId assetId, boolean deleteBlob) {
        Set<String> result = super.deleteAssetTx(assetId, deleteBlob);
        afterDelete();
        return result;
    }


    private void afterDelete() {
        try {
            getRepository().facet(CondaFacet.class).rebuildRepoDataJson();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
