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
public class CloudPlatformBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudPlatformBasedUpgradeImageFilter.class);

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        String cloudPlatform = imageFilterParams.getCloudPlatform();
        List<Image> filteredImages = filterImages(imageFilterResult, cloudPlatform);
        LOGGER.debug("After the filtering {} image found with {} cloud platform.", filteredImages.size(), cloudPlatform);
        return new ImageFilterResult(filteredImages, getReason(filteredImages, cloudPlatform));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, String cloudPlatform) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> image.getImageSetsByProvider().keySet().stream().anyMatch(key -> key.equalsIgnoreCase(cloudPlatform)))
                .collect(Collectors.toList());
    }

    private String getReason(List<Image> filteredImages, String cloudPlatform) {
        return filteredImages.isEmpty() ? String.format("There are no eligible images to upgrade for %s cloud platform.", cloudPlatform) : EMPTY_REASON;
    }
}
