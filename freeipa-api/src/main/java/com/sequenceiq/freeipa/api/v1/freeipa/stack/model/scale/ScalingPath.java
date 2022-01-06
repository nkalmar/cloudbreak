package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;

public class ScalingPath {

    private FormFactor originalFormFactor;

    private FormFactor targetFormFactor;

    public ScalingPath(FormFactor originalFormFactor, FormFactor targetFormFactor) {
        this.originalFormFactor = originalFormFactor;
        this.targetFormFactor = targetFormFactor;
    }

    public FormFactor getOriginalFormFactor() {
        return originalFormFactor;
    }

    public void setOriginalFormFactor(FormFactor originalFormFactor) {
        this.originalFormFactor = originalFormFactor;
    }

    public FormFactor getTargetFormFactor() {
        return targetFormFactor;
    }

    public void setTargetFormFactor(FormFactor targetFormFactor) {
        this.targetFormFactor = targetFormFactor;
    }

    @Override
    public String toString() {
        return "ScalingPath{" +
                "originalFormFactor=" + originalFormFactor +
                ", targetFormFactor=" + targetFormFactor +
                '}';
    }
}
