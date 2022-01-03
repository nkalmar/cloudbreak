package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradePermissionProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.UpgradeImageFilter;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;

@Component
public class CmAndStackVersionUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmAndStackVersionUpgradeImageFilter.class);

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private UpgradePermissionProvider upgradePermissionProvider;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, imageFilterParams);
        LOGGER.debug("After the filtering {} image left with proper CM and CDH version.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, imageFilterParams));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> isUpgradePermitted(imageFilterParams, image))
                .collect(Collectors.toList());
    }

    private boolean isUpgradePermitted(ImageFilterParams imageFilterParams, Image candidateImage) {
        return imageFilterParams.isLockComponents()
                ? lockedComponentChecker.isUpgradePermitted(imageFilterParams.getCurrentImage(), candidateImage, imageFilterParams.getStackRelatedParcels())
                : isUnlockedCmAndStackUpgradePermitted(imageFilterParams, candidateImage);
    }

    private boolean isUnlockedCmAndStackUpgradePermitted(ImageFilterParams imageFilterParams, Image candidateImage) {
        return upgradePermissionProvider.permitCmUpgrade(imageFilterParams, candidateImage)
                && upgradePermissionProvider.permitStackUpgrade(imageFilterParams, candidateImage);
    }

    private String getReason(List<Image> filteredImages, ImageFilterParams imageFilterParams) {
        return filteredImages.isEmpty() ? getMessage(imageFilterParams) : EMPTY_REASON;
    }

    private String getMessage(ImageFilterParams imageFilterParams) {
        return imageFilterParams.isLockComponents()
                ? "There is at least one activated parcel for which we cannot find image with matching version. " + "Activated parcel(s): "
                        + imageFilterParams.getStackRelatedParcels()
                : "There is no proper Cloudera Manager or CDP version to upgrade.";
    }
}
