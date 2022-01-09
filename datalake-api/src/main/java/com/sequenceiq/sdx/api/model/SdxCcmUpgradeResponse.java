package com.sequenceiq.sdx.api.model;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class SdxCcmUpgradeResponse {

    private FlowIdentifier flowIdentifier;

    private CcmUpgradeResponseType responseType;

    private String reason;

    public SdxCcmUpgradeResponse() {
    }

    public SdxCcmUpgradeResponse(CcmUpgradeResponseType responseType, FlowIdentifier flowIdentifier, String reason) {
        this.responseType = responseType;
        this.flowIdentifier = flowIdentifier;
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public CcmUpgradeResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(CcmUpgradeResponseType responseType) {
        this.responseType = responseType;
    }

    @Override
    public String toString() {
        return "SdxCcmUpgradeResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                '}';
    }
}
