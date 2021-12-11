package com.sequenceiq.datalake.service.sdx.attach;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class detaches the data lake from the environment.
 * <p>
 * It is achieved by renaming name and cluster CRN. CLuster associated with the stack is also renamed as well.
 * After this operation is performed, the stack associated with this cluster is also renamed so that DL name and stack same are the same.
 * <p>
 */
@Component
public class SdxDetachService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDetachService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private SdxDetachNameGenerator sdxDetachNameGenerator;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    /**
     * Detaches the internal SDX cluster by assigning it a new "detached" name and CRN.
     */
    public SdxCluster detachCluster(SdxCluster cluster) {
        LOGGER.info("Started detaching SDX cluster with ID: {}.", cluster.getId());

        String originalName = cluster.getClusterName();
        modifyClusterNameAndCrnForDetach(cluster, originalName);
        cluster.setDetached(true);
        SdxCluster saved = sdxClusterRepository.save(cluster);

        LOGGER.info("Finished detaching SDX cluster with ID: {}. Modified name from {} to {} and crn from {} to {}.",
                saved.getId(), originalName, saved.getName(), saved.getOriginalCrn(), saved.getCrn());
        return saved;
    }

    private void modifyClusterNameAndCrnForDetach(SdxCluster cluster, String originalName) {
        // We transform the name and CRN in order to use the original values for replacement clusters.
        String originalCrn = cluster.getCrn();
        cluster.setCrn(regionAwareCrnGenerator.generateCrnStringWithUuid(
                CrnResourceDescriptor.DATALAKE, cluster.getAccountId()
        ));
        cluster.setClusterName(sdxDetachNameGenerator.generateDetachedClusterName(originalName));

        // Retain in case we need to reattach the cluster later.
        cluster.setOriginalCrn(originalCrn);
    }

    /**
     * Detaches the backing stack of the SDX cluster by updating its name and CRN to the new ones
     * for the detached cluster.
     *
     * Can throw the following exceptions:
     *      - NotFoundException : Internal stack is not found by the cluster's name.
     *      - TransactionExecutionException : If updating the stack fails due to the stack not being
     *              able to be saved due to the stack object being null.
     */
    public void detachStack(SdxCluster cluster, String originalName) throws Exception {
        LOGGER.info("Started detaching stack of SDX cluster with ID: {}.", cluster.getId());

        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.updateNameAndCrn(0L, originalName, initiatorUserCrn, cluster.getName(), cluster.getCrn())
        );

        LOGGER.info("Finished detaching stack of SDX cluster with ID: {}.", cluster.getId());
    }

    /**
     * "Detaches" the external database of the SDX cluster by giving it the new detached CRN of the cluster.
     *
     * Can throw the following exceptions:
     *      - NotFoundException : If the database is not found for the cluster CRN and environment CRN.
     *      - IllegalArgumentException : If the database could not be saved due to the object for it being null.
     */
    public void detachExternalDatabase(SdxCluster cluster) throws Exception {
        LOGGER.info("Started detaching external database of SDX cluster with ID {} so it has crn {} " +
                "instead of {}.", cluster.getId(), cluster.getCrn(), cluster.getOriginalCrn());

        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                redbeamsServerEndpoint.updateClusterCrn(
                        cluster.getEnvCrn(), cluster.getOriginalCrn(), cluster.getCrn(), initiatorUserCrn
                )
        );

        LOGGER.info("Finished detaching external database of SDX cluster with ID {}.", cluster.getId());
    }
}
