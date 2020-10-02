package org.icgc_argo.workflowingestionnode;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WorkflowIngestionNodeApplicationTests {

  @Test
  @Disabled // Disabled because context load will fail with out rabbit and kafka running.
  void contextLoads() {}
}
