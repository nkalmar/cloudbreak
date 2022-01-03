package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@Component
public class CurrentImageUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentImageUpgradeImageFilter.class);

    @Inject
    private ImageProvider imageProvider;

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        List<Image> filteredImages = filterImages(imageFilterResult, imageFilterParams);
        LOGGER.debug("After the filtering {} image left.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        return imageFilterResult.getImages()
                .stream()
                .filter(filterCurrentImage(imageFilterParams))
                .collect(Collectors.toList());
    }

    private Predicate<Image> filterCurrentImage(ImageFilterParams imageFilterParams) {
        return image -> {
            String currentImageId = imageFilterParams.getCurrentImage().getUuid();
            if (!(image.getUuid().equals(currentImageId) && !imageProvider.filterCurrentImage(imageFilterParams.getStackId(), currentImageId))) {
                LOGGER.debug("The current image was removed from the upgrade candidates.");
                return true;
            } else {
                return false;
            }
        };
    }

    private String getReason(List<Image> filteredImages) {
        return filteredImages.isEmpty() ? "There are no newer compatible images available." : EMPTY_REASON;
    }
}
