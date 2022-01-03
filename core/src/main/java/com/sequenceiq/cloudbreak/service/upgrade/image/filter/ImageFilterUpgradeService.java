package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Service
public class ImageFilterUpgradeService {

    public static final String EMPTY_REASON = "";

    @Inject
    private Set<UpgradeImageFilter> upgradeImageFilters;

    public ImageFilterResult filterImages(List<Image> availableImages, ImageFilterParams imageFilterParams) {
        ImageFilterResult result = new ImageFilterResult(availableImages, EMPTY_REASON);
        for (UpgradeImageFilter imageFilter : upgradeImageFilters) {
            if (result.getImages().isEmpty()) {
                return result;
            } else {
                result = imageFilter.filter(result, imageFilterParams);
            }
        }
        return result;
    }
}
