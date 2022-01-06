package com.sequenceiq.freeipa.service.stack;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.service.upgrade.UpgradeImageService;

@ExtendWith(MockitoExtension.class)
class FreeIpaScalingServiceTest {

    @Mock
    private OperationService operationService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private UpgradeImageService imageService;

    @Mock
    private FreeIpaScalingValidationService validationService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @InjectMocks
    private FreeIpaScalingService underTest;

}