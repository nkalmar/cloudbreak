package com.sequenceiq.cloudbreak.service.upgrade.image;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class BlueprintBasedUpgradeValidator {

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    public BlueprintValidationResult isValidBlueprint(ImageFilterParams imageFilterParams, String accountId) {
        if (imageFilterParams.getStackType().equals(StackType.DATALAKE)) {
            boolean mediumDuty = imageFilterParams.getBlueprint().getName().contains("SDX Medium Duty");
            boolean canUpgradeMediumDuty = mediumDuty && entitlementService.haUpgradeEnabled(accountId);
            return new BlueprintValidationResult(!mediumDuty || canUpgradeMediumDuty, "The upgrade is not allowed for this template.");
        } else {
            return blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams.getBlueprint(), imageFilterParams.isLockComponents(),
                    imageFilterParams.isSkipValidations(), imageFilterParams.isDataHubUpgradeEntitled());
        }
    }
}
