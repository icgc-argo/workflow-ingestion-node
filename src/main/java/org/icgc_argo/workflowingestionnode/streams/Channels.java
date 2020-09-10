package org.icgc_argo.workflowingestionnode.streams;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface Channels {
    String INPUT = "inbound";
    String OUTPUT = "outbound";

    @Input(INPUT)
    SubscribableChannel analysisPublishEvents();

    @Output(OUTPUT)
    MessageChannel startQueue();
}