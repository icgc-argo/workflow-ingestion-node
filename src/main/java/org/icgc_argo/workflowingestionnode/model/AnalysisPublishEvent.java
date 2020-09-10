package org.icgc_argo.workflowingestionnode.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @Builder
public class AnalysisPublishEvent {
    private String analysisType;
    private String analysisId;
}