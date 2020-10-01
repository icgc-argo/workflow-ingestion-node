package org.icgc_argo.workflowingestionnode.config;

import lombok.Getter;
import org.icgc_argo.workflow_graph_lib.workflow.client.RdpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Value("${rdpc.url}")
  @Getter
  private String rdpcUrl;

  @Bean
  public RdpcClient createRdpcClient() {
    return new RdpcClient(rdpcUrl);
  }
}
