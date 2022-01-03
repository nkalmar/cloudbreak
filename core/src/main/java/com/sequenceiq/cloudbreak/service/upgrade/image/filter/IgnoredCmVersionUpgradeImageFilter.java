package com.sequenceiq.cloudbreak.service.upgrade.image.filter;


import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class IgnoredCmVersionUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoredCmVersionUpgradeImageFilter.class);

    private static final String IGNORED_CM_VERSION = "7.x.0";

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult);
        LOGGER.debug("After the filtering {} image left with proper Cloudera Manager version format.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> !image.getPackageVersion(ImagePackageVersion.CM).contains(IGNORED_CM_VERSION))
                .collect(Collectors.toList());
    }

    private String getReason(List<Image> filteredImages) {
        return filteredImages.isEmpty() ? "There are no eligible images with supported Cloudera Manager or CDP version." : EMPTY_REASON;
    }
}
