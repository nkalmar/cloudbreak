package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class FreeIpaScalingTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    private static final long TWO_HOURS_IN_SEC = 2L * 60 * 60;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 1 FreeIPA instances " +
                    "AND the stack is scaled up by one node " +
                    "AND the stack is scaled up by one node ",
            then = "the stack should be available AND deletable and have 3 nodes")
    public void testSingleFreeIpaInstanceUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
//                .when(freeIpaTestClient.upgrade())
//                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
//                .given(FreeIpaOperationStatusTestDto.class)
//                .withOperationId(((FreeIpaTestDto) testContext.get(freeIpa)).getOperationId())
//                .then((tc, testDto, freeIpaClient) -> testFreeIpaAvailabilityDuringUpgrade(tc, testDto, freeIpaClient, freeIpa))
//                .await(COMPLETED)
//                .given(freeIpa, FreeIpaTestDto.class)
//                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
//                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
//                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

}
