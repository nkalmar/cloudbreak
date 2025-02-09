package com.sequenceiq.cloudbreak.telemetry.common;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.PropertyPlaceholderHelper;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.VmLog;

@Service
public class TelemetryCommonConfigService {

    static final String SERVICE_LOG_FOLDER_PREFIX = "serviceLogFolderPrefix";

    static final String AGENT_LOG_FOLDER_PREFIX = "agentLogFolderPrefix";

    static final String SERVER_LOG_FOLDER_PREFIX = "serverLogFolderPrefix";

    private static final String LOG_FOLDER_DEFAULT = "/var/log";

    private final AnonymizationRuleResolver anonymizationRuleResolver;

    public TelemetryCommonConfigService(AnonymizationRuleResolver anonymizationRuleResolver) {
        this.anonymizationRuleResolver = anonymizationRuleResolver;
    }

    public TelemetryCommonConfigView createTelemetryCommonConfigs(Telemetry telemetry, List<VmLog> logs,
            TelemetryClusterDetails clusterDetails) {
        resolveLogPathReferences(telemetry, logs);
        return new TelemetryCommonConfigView.Builder()
                .withClusterDetails(clusterDetails)
                .withRules(anonymizationRuleResolver.decodeRules(telemetry.getRules()))
                .withVmLogs(logs)
                .build();
    }

    @VisibleForTesting
    void resolveLogPathReferences(Telemetry telemetry, List<VmLog> logs) {
        Map<String, Object> fluentAttributes = telemetry.getFluentAttributes();
        if (CollectionUtils.isNotEmpty(logs)) {
            Properties props = new Properties();
            props.setProperty(SERVER_LOG_FOLDER_PREFIX, fluentAttributes.getOrDefault(SERVER_LOG_FOLDER_PREFIX, LOG_FOLDER_DEFAULT).toString());
            props.setProperty(AGENT_LOG_FOLDER_PREFIX, fluentAttributes.getOrDefault(AGENT_LOG_FOLDER_PREFIX, LOG_FOLDER_DEFAULT).toString());
            props.setProperty(SERVICE_LOG_FOLDER_PREFIX, fluentAttributes.getOrDefault(SERVICE_LOG_FOLDER_PREFIX, LOG_FOLDER_DEFAULT).toString());
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            for (VmLog log : logs) {
                String resolvedPath = propertyPlaceholderHelper.replacePlaceholders(log.getPath(), props);
                log.setPath(resolvedPath);
            }
        }
    }
}
