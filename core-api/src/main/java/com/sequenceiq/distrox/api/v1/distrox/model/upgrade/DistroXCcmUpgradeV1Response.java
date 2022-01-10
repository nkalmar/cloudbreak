package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class DistroXCcmUpgradeV1Response {

    private String reason;

    private FlowIdentifier flowIdentifier;

    public DistroXCcmUpgradeV1Response() {
    }

    public DistroXCcmUpgradeV1Response(String reason, FlowIdentifier flowIdentifier) {
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void appendReason(String reason) {
        if (StringUtils.isNotEmpty(this.reason)) {
            this.reason += reason;
        } else {
            setReason(reason);
        }
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "DistroXUpgradeV1Response{" +
                "reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
