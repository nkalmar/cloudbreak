package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.configuration.AllowedScalingPaths;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaScalingValidationServiceTest {

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AllowedScalingPaths allowedScalingPaths;

    @InjectMocks
    private FreeIpaScalingValidationService underTest;

    @Test
    public void testUpscaleIfNoInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(Set.of(), stack, null));
        assertEquals("There are no instances available for scaling!", exception.getMessage());
    }

    @Test
    public void testUpscaleIfMoreInstancesExistsThenValidationFails() {
        Stack stack = mock(Stack.class);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(createValidImSet(4), stack, null));
        assertEquals("Upscaling currently only available for FreeIPA installation with 1 or 2 instances", exception.getMessage());
    }

    @Test
    public void testUpscaleIfUnavailableInstanceExistsThenValidationFails() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> validImSet = createValidImSet(2);
        validImSet.removeIf(instanceMetaData -> instanceMetaData.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
        InstanceMetaData pgw = createPrimaryGateway();
        validImSet.add(pgw);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateStackForUpscale(validImSet, stack, null));
        assertEquals("Some of the instances is not available. Please fix them first! Instances: [pgw]", exception.getMessage());
    }

    private InstanceMetaData createPrimaryGateway() {
        InstanceMetaData pgw = new InstanceMetaData();
        pgw.setInstanceStatus(InstanceStatus.UNREACHABLE);
        pgw.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        pgw.setInstanceId("pgw");
        return pgw;
    }

    private Set<InstanceMetaData> createValidImSet(int instanceCount) {
        Set<InstanceMetaData> set = new HashSet<>();
        for (int i = 1; i <= instanceCount; i++) {
            InstanceMetaData im = new InstanceMetaData();
            if (i == 1) {
                im.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                im.setInstanceId("pgw");
            } else {
                im.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                im.setInstanceId("im" + i);
            }
            im.setInstanceStatus(InstanceStatus.CREATED);
            set.add(im);
        }
//        InstanceMetaData im1 = new InstanceMetaData();
//        im1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
//        im1.setInstanceId("pgw");
//        im1.setInstanceStatus(InstanceStatus.CREATED);
//        InstanceMetaData im2 = new InstanceMetaData();
//        im2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
//        im2.setInstanceId("im2");
//        im2.setInstanceStatus(InstanceStatus.CREATED);
//        set.add(im1);
//        set.add(im2);

//        return Set.of(im1, im2);
        return set;
    }
}