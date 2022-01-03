package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeImageFilterTest {

    private static final String CLOUD_PLATFORM = "aws";

    private static final String CATALOG_NAME = "catalog name";

    private static final Long WORKSPACE_ID = 1L;

    private static final long STACK_ID = 1L;

    private static final String ACCOUNT_ID = "account1";

    private static final String BLUEPRINT_ERROR = "Invalid blueprint";

    @InjectMocks
    private ClusterUpgradeImageFilter underTest;

    @Mock
    private BlueprintBasedUpgradeValidator blueprintBasedUpgradeValidator;

    @Mock
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageFilterUpgradeService imageFilterUpgradeService;

    @Mock
    private CloudbreakImageCatalogV3 imageCatalogV3;

    @Mock
    private Image image;

    private final ImageFilterParams imageFilterParams = createImageFilterParams();

    @Test
    void testFilterWithImageCatalogShouldReturnErrorMessageWhenTheBlueprintIsNotEligible() {
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(false, BLUEPRINT_ERROR));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, imageCatalogV3, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BLUEPRINT_ERROR, actual.getReason());
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verifyNoInteractions(imageCatalogServiceProxy);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterShouldReturnErrorMessageWhenTheBlueprintIsNotEligible() {
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(false, BLUEPRINT_ERROR));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BLUEPRINT_ERROR, actual.getReason());
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verifyNoInteractions(imageCatalogServiceProxy);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterWithImageCatalogShouldReturnErrorMessageWhenThereAreNoAvailableCandidateImage() {
        String errorMessage = "There are no available image";
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogServiceProxy.getImageFilterResult(imageCatalogV3)).thenReturn(new ImageFilterResult(Collections.emptyList(), errorMessage));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, imageCatalogV3, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(errorMessage, actual.getReason());
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verify(imageCatalogServiceProxy).getImageFilterResult(imageCatalogV3);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterWithImageCatalogShouldReturnFilteredImages() {
        List<Image> images = List.of(image);
        ImageFilterResult result = new ImageFilterResult(images, EMPTY_REASON);
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogServiceProxy.getImageFilterResult(imageCatalogV3)).thenReturn(result);
        when(imageFilterUpgradeService.filterImages(images, imageFilterParams)).thenReturn(result);

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, imageCatalogV3, imageFilterParams);

        assertEquals(result, actual);
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verify(imageCatalogServiceProxy).getImageFilterResult(imageCatalogV3);
        verify(imageFilterUpgradeService).filterImages(images, imageFilterParams);
        verifyNoInteractions(imageCatalogService);
    }

    @Test
    void testFilterShouldReturnFilteredImages() throws CloudbreakImageCatalogException {
        List<Image> images = List.of(image);
        ImageFilterResult result = new ImageFilterResult(images, EMPTY_REASON);
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(true));
        when(imageFilterUpgradeService.filterImages(images, imageFilterParams)).thenReturn(result);
        when(imageCatalogService.getImages(WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM)).thenReturn(createStatedImages(images));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, CATALOG_NAME, imageFilterParams);

        assertEquals(result, actual);
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verify(imageCatalogService).getImages(WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM);
        verify(imageFilterUpgradeService).filterImages(images, imageFilterParams);
        verifyNoInteractions(imageCatalogServiceProxy);
    }

    @Test
    void testFilterShouldReturnErrorMessageWhenImageCatalogThrowsAnException() throws CloudbreakImageCatalogException {
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogService.getImages(WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM)).thenThrow(new CloudbreakImageCatalogException(""));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("Failed to retrieve eligible images due tue an internal error.", actual.getReason());
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verify(imageCatalogService).getImages(WORKSPACE_ID, CATALOG_NAME, CLOUD_PLATFORM);
        verifyNoInteractions(imageFilterUpgradeService);
        verifyNoInteractions(imageCatalogServiceProxy);
    }

    private StatedImages createStatedImages(List<Image> images) {
        return StatedImages.statedImages(new Images(null, images, null, null), "", CATALOG_NAME);
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(image, false, Collections.emptyMap(), StackType.DATALAKE, new Blueprint(), STACK_ID,
                new InternalUpgradeSettings(false, true, true), CLOUD_PLATFORM);
    }

}