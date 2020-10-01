package org.icgc_argo.workflowingestionnode.streams;

import lombok.val;
import org.icgc_argo.workflow_graph_lib.schema.GraphEvent;
import org.springframework.cloud.schema.registry.avro.AvroSchemaMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

@Component
public class MessageConverters {
  @Bean
  public MessageConverter avroMessageConverter() {
    val converter =
        new AvroSchemaMessageConverter(new MimeType("application", "vnd.GraphEvent+avro"));
    converter.setSchema(GraphEvent.getClassSchema());
    return converter;
  }
}
