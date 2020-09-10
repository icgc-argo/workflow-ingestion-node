package org.icgc_argo.workflowingestionnode.streams;


import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflowingestionnode.model.AnalysisPublishEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.integration.annotation.Filter;
import org.springframework.messaging.Message;

@EnableBinding(Channels.class)
@Slf4j
public class Config {
    private static final String ACCEPTED_ANALYSIS_TYPE = "sequencing_alignment";

    @Filter(inputChannel = Channels.INPUT, outputChannel = Channels.OUTPUT)
    public boolean analysisPublishEventToStartQueue(Message<AnalysisPublishEvent> message) {
        log.debug("Msg recieved {}", message);

        val analysisPublishEvent = message.getPayload();

        return analysisPublishEvent.getAnalysisType().toLowerCase().equals(ACCEPTED_ANALYSIS_TYPE);
    }
}
