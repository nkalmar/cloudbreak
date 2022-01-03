package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

public interface UpgradeImageFilter {

    ImageFilterResult filter(ImageFilterResult imageFilterResult, ImageFilterParams imageFilterParams);
}
