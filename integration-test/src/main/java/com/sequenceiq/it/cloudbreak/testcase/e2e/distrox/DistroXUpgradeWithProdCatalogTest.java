package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.distrox.AvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;
import com.sequenceiq.it.cloudbreak.util.UpgradeMatrixUtil;

public class DistroXUpgradeWithProdCatalogTest extends AbstractE2ETest {

    private  static final String PRODCATALOGNAME = "prod-catalog";

    private String currentRuntimeVersion;

    private String targetRuntimeVersion;

    private String sdxName;

    private String distroXName;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTest;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    private String uuid;

    @Override
    protected void setupTest(TestContext testContext) {
        String prodCatalogUrl = commonClusterManagerProperties.getUpgrade().getImageCatalogUrl3rdParty();
        createDefaultUser(testContext);
        createImageValidationSourceCatalog(testContext, prodCatalogUrl, PRODCATALOGNAME);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
        setupUpgradeParameters();
    }

    protected void setupUpgradeParameters() {
        UpgradeMatrixUtil.UpgradeVersionsContainer uvc = UpgradeMatrixUtil.getRandomRuntimeVersions();
        currentRuntimeVersion = uvc.getCurrentVersion();
        targetRuntimeVersion = uvc.getTargetVersion();
        sdxName = resourcePropertyProvider().getName();
        distroXName = resourcePropertyProvider().getName();
    }

    protected String getUuid(TestContext testContext) {
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        return dto.getResponse().getImages().getCdhImages().stream()
                .filter(img -> img.getVersion().equals(currentRuntimeVersion) && img.getImageSetsByProvider().keySet().iterator().next()
                        .equals(testContext.commonCloudProperties().getCloudProvider().toLowerCase())).iterator().next().getUuid();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "upgrade called on the DistroX cluster with 3rd party image settings", then = "DistroX upgrade should be successful," +
            " the cluster should be up and running")
    public void testDistroXUpgradeProdCatalog(TestContext testContext) {
        String imageSettings = resourcePropertyProvider().getName();
        testContext
                .given(ImageCatalogTestDto.class).withName(PRODCATALOGNAME)
                .when(imageCatalogTest.getV4(true))
                .valid();
                uuid = getUuid(testContext);
        testContext
                .given(sdxName, SdxTestDto.class)
                .withRuntimeVersion(currentRuntimeVersion)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.create(), key(sdxName))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(imageSettings, DistroXImageTestDto.class).withImageCatalog(PRODCATALOGNAME).withImageId(uuid)
                .given(distroXName, DistroXTestDto.class)
                .withTemplate(String.format(commonClusterManagerProperties.getInternalDistroXBlueprintType(), currentRuntimeVersion))
                .withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst()
                .withImageSettings(imageSettings)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(new AvailabilityZoneAssertion())
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.stop(), key(distroXName))
                .await(STACK_STOPPED)
                .validate();
        testContext
                .given(SdxUpgradeTestDto.class)
                .withReplaceVms(SdxUpgradeReplaceVms.DISABLED)
                .withRuntime(targetRuntimeVersion)
                .given(sdxName, SdxTestDto.class)
                .when(sdxTestClient.upgrade(), key(sdxName))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, key(sdxName).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxName))
                .awaitForHealthyInstances()
                .validate();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.start(), key(distroXName))
                .await(STACK_AVAILABLE)
                .validate();
        testContext
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .then(new AvailabilityZoneAssertion())
                .validate();
    }
}