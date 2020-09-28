package org.icgc_argo.workflowingestionnode.rdpc;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RdpcConfig {
  @Value("${rdpc.url}")
  @Getter
  private String rdpcUrl;
}
