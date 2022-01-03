package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
public class NonCmUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonCmUpgradeImageFilter.class);

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult);
        LOGGER.debug("After the filtering {} image left with Cloudera Manager package.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult) {
        return imageFilterResult.getImages()
                .stream()
                .filter(image -> isNotEmpty(image.getPackageVersion(ImagePackageVersion.CM)))
                .collect(Collectors.toList());
    }

    private String getReason(List<Image> filteredImages) {
        return filteredImages.isEmpty() ? "There are no eligible images to upgrade available with Cloudera Manager packages." : EMPTY_REASON;
    }
}
