package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService.EMPTY_REASON;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.UpgradeImageFilter;

@Component
public class ImageCreationBasedUpgradeImageFilter implements UpgradeImageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCreationBasedUpgradeImageFilter.class);

    @Override
    public ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams) {
        Image currentImage = imageFilterParams.getCurrentImage();
        List<Image> filteredImages = filterImages(imageFilterResult, currentImage);
        LOGGER.debug("After the filtering {} image left with proper creation date.", filteredImages.size());
        return new ImageFilterResult(filteredImages, getReason(filteredImages, currentImage));
    }

    private List<Image> filterImages(ImageFilterResult imageFilterResult, Image currentImage) {
        return imageFilterResult.getImages()
                .stream()
                .filter(candidate -> isDifferentVersion(currentImage, candidate) || isNewerOrSameCreationImage(currentImage, candidate))
                .collect(Collectors.toList());
    }

    private boolean isDifferentVersion(Image currentImage, Image candidate) {
        return Objects.nonNull(currentImage.getStackDetails())
                && StringUtils.isNotBlank(currentImage.getStackDetails().getVersion())
                && Objects.nonNull(candidate.getStackDetails())
                && StringUtils.isNotBlank(candidate.getStackDetails().getVersion())
                && !candidate.getStackDetails().getVersion().equalsIgnoreCase(currentImage.getStackDetails().getVersion());
    }

    private boolean isNewerOrSameCreationImage(Image currentImage, Image candidate) {
        return Objects.nonNull(candidate.getCreated())
                && Objects.nonNull(currentImage.getCreated())
                && candidate.getCreated() >= currentImage.getCreated();
    }

    private String getReason(List<Image> filteredImages, Image currentImage) {
        return filteredImages.isEmpty() ? "There are no newer images available than " + currentImage.getDate() + "." : EMPTY_REASON;
    }
}
