package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class OsVersionBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsVersionBasedUpgradeImageFilter.class);

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String currentOs = imageFilterParams.getCurrentImage().getOs();
        String currentOsType = imageFilterParams.getCurrentImage().getOsType();
        List<Image> filteredImages = filterImages(imageFilterResult, currentOs, currentOsType);
        LOGGER.debug("After the filtering {} image left with proper OS {} and OS type {}.", filteredImages.size(), currentOs, currentOsType);
        return new ImageFilterResult(filteredImages, getReason(filteredImages));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, String currentOs, String currentOsType) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> isOsVersionsMatch(currentOs, currentOsType, image))
                .collect(Collectors.toList());
    }

    private boolean isOsVersionsMatch(String currentOs, String currentOsType, Image newImage) {
        return newImage.getOs().equalsIgnoreCase(currentOs) && newImage.getOsType().equalsIgnoreCase(currentOsType);
    }

    private String getReason(List<Image> filteredImages) {
        return filteredImages.isEmpty() ? "There are no eligible images to upgrade with the same OS version." : EMPTY_REASON;
    }
}
