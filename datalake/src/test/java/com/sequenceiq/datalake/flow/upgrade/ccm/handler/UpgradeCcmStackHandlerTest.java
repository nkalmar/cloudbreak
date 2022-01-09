package com.sequenceiq.datalake.flow.upgrade.ccm.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackRequest;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmSuccessEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.upgrade.ccm.SdxCcmUpgradeService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmStackHandlerTest {

    @Mock
    private SdxCcmUpgradeService ccmUpgradeService;

    @Mock
    private EventBus eventBus;

    @Captor
    private ArgumentCaptor<Object> keyCaptor;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    @InjectMocks
    private UpgradeCcmStackHandler underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "sleepTimeInSec", 1);
        ReflectionTestUtils.setField(underTest, "durationInMinutes", 2);
        lenient().doAnswer(i -> null).when(eventBus).notify(keyCaptor.capture(), eventCaptor.capture());
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UpgradeCcmStackRequest");
    }

    @Test
    void defaultFailureEvent() {
        Selectable failureEvent = underTest.defaultFailureEvent(1L, new Exception("error"), new Event<>(new UpgradeCcmStackRequest(1L, "user")));
        assertThat(failureEvent.selector()).isEqualTo("UpgradeCcmFailedEvent");
    }

    @Test
    void acceptSuccess() {
        UpgradeCcmStackRequest request = new UpgradeCcmStackRequest(1L, "user");
        Event.Headers headers = new Event.Headers();
        Event<UpgradeCcmStackRequest> event = new Event<>(headers, request);
        underTest.accept(event);

        UpgradeCcmSuccessEvent successEvent = new UpgradeCcmSuccessEvent(1L, "user");

        Event<?> capturedEvent = eventCaptor.getValue();
        assertThat(keyCaptor.getValue()).isEqualTo(successEvent.getClass().getSimpleName());
        assertThat(capturedEvent.getHeaders()).isEqualTo(headers);
        assertThat(capturedEvent.getData()).usingRecursiveComparison().isEqualTo(successEvent);
    }

    @ParameterizedTest
    @ValueSource(classes = { UserBreakException.class, PollerStoppedException.class, PollerException.class, RuntimeException.class })
    void acceptWithExceptions(Class<? extends Throwable> errorClass) {
        UpgradeCcmStackRequest request = new UpgradeCcmStackRequest(1L, "user");
        Event.Headers headers = new Event.Headers();
        Event<UpgradeCcmStackRequest> event = new Event<>(headers, request);

        doThrow(errorClass).when(ccmUpgradeService).initAndWaitForStackUpgrade(anyLong(), any(PollingConfig.class));
        underTest.accept(event);

        UpgradeCcmFailedEvent failedEvent = new UpgradeCcmFailedEvent(1L, "user", new Exception("error"));

        Event<?> capturedEvent = eventCaptor.getValue();
        assertThat(keyCaptor.getValue()).isEqualTo(failedEvent.getClass().getSimpleName());
        assertThat(capturedEvent.getHeaders()).isEqualTo(headers);
        assertThat(capturedEvent.getData()).usingRecursiveComparison().isEqualTo(failedEvent);
    }

}
