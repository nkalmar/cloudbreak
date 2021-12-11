package com.sequenceiq.datalake.service.sdx.attach;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provides core logic and functionality for attaching or reattaching an SDX cluster.
 */
@Component
public class SdxAttachService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAttachService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private SdxDetachNameGenerator sdxDetachNameGenerator;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint redbeamsServerEndpoint;

    /**
     * Central logic for attaching an SDX cluster. Specifically just saves the cluster and gives it the resource
     * owner role.
     *
     * Note that this method returns a possibly different version of the original cluster, so the original should
     * not be used anymore.
     *
     * Will throw a TransactionExecutionException if:
     *      - Cluster cannot be saved.
     *      - The resource owner role assignment fails its UMS GRPC call.
     *
     * Does not throw an exception if the owner role is already assigned to the provided cluster.
     */
    public SdxCluster attachSdx(SdxCluster cluster) throws Exception {
        return transactionService.required(() -> {
            SdxCluster created = sdxClusterRepository.save(cluster);
            ownerAssignmentService.assignResourceOwnerRoleIfEntitled(
                    created.getInitiatorUserCrn(), created.getCrn(), created.getAccountId()
            );
            return created;
        });
    }

    /**
     * Central logic for reattaching an SDX cluster.
     *
     * Can throw the following exceptions:
     *      - Exceptions thrown by reattachCluster.
     *      - Exceptions thrown by reattachStack.
     *      - Exceptions thrown by reattachExternalDatabase.
     *      - Exceptions thrown by attachSdx.
     */
    public SdxCluster reattachSdx(SdxCluster clusterToReattach) throws Exception {
        String detachedName = clusterToReattach.getName();
        clusterToReattach = reattachCluster(clusterToReattach);
        reattachStack(clusterToReattach, detachedName);

        if (clusterToReattach.hasExternalDatabase()) {
            reattachExternalDatabase(clusterToReattach);
        }

        clusterToReattach.setOriginalCrn(clusterToReattach.getCrn());
        clusterToReattach = attachSdx(clusterToReattach);
        return clusterToReattach;
    }

    /**
     * Reattaches the internal SDX cluster by reverting its name and CRN to original values.
     *
     * Throws RuntimeException if cluster is not detached or could not revert name and CRN.
     */
    public SdxCluster reattachCluster(SdxCluster cluster) throws Exception {
        LOGGER.info("Started reattaching SDX cluster with ID: {}", cluster.getId());

        if (!cluster.isDetached()) {
            throw new RuntimeException("Attempting to reattach a cluster which was not detached!");
        }

        modifyClusterNameAndCrnForReattach(cluster);
        cluster.setDetached(false);
        SdxCluster saved = sdxClusterRepository.save(cluster);

        LOGGER.info("Finished reattaching SDX cluster with ID: {}. Cluster now has name {} and crn {}.",
                saved.getId(), saved.getName(), saved.getCrn());
        return saved;
    }

    /**
     * Gives the cluster being reattached its original name and CRN.
     *
     * Throws RuntimeException if the cluster's name is malformed from what is expected for a detached cluster.
     */
    private void modifyClusterNameAndCrnForReattach(SdxCluster cluster) throws Exception {
        String detachedCrn = cluster.getOriginalCrn();
        cluster.setCrn(detachedCrn);
        // TODO: I just did this so we can retain it in case we need it, but don't really see how. Should I just set this to null?
        cluster.setOriginalCrn(detachedCrn);
        cluster.setClusterName(sdxDetachNameGenerator.generateOriginalNameFromDetached(cluster.getName()));
    }

    /**
     * Reattaches the backing stack of the SDX cluster by reverting its name and CRN.
     *
     * Can throw the following exceptions:
     *      - NotFoundException : Internal stack is not found by the cluster's name.
     *      - TransactionExecutionException : If updating the stack fails.
     */
    public void reattachStack(SdxCluster cluster, String originalName) throws Exception {
        LOGGER.info("Started reattaching stack of SDX cluster with ID: {}", cluster.getId());

        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.updateNameAndCrn(
                        0L, originalName, initiatorUserCrn, cluster.getName(), cluster.getCrn()
                )
        );

        LOGGER.info("Finished reattaching stack of SDX cluster with ID: {}. Now has name {} and crn {}.",
                cluster.getId(), cluster.getName(), cluster.getCrn());
    }

    /**
     * Reattaches the external database of the SDX cluster by reverting its CRN.
     *
     * Throws a NotFoundException if the database is not found for the cluster CRN and environment CRN.
     */
    public void reattachExternalDatabase(SdxCluster cluster) throws Exception {
        LOGGER.info("Started reattaching external database for SDX cluster with ID: {}", cluster.getId());

        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                redbeamsServerEndpoint.updateClusterCrn(
                        cluster.getEnvCrn(), cluster.getOriginalCrn(), cluster.getCrn(), initiatorUserCrn
                )
        );

        LOGGER.info("Finished reattaching external database for SDX cluster with ID: {}. Now has crn: {}.",
                cluster.getId(), cluster.getCrn());
    }
}
