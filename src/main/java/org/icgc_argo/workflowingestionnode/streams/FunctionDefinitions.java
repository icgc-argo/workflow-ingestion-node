/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc_argo.workflowingestionnode.streams;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.schema.AnalysisFile;
import org.icgc_argo.workflow_graph_lib.schema.GraphEvent;
import org.icgc_argo.workflowingestionnode.model.Analysis;
import org.icgc_argo.workflowingestionnode.model.AnalysisEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FunctionDefinitions {
  @Value("function.analysisToGraphEvent.acceptedAnalysisState")
  private String ACCEPTED_ANALYSIS_STATE;
  @Value("function.analysisToGraphEvent.acceptedAnalysisType")
  private String ACCEPTED_ANALYSIS_TYPE;

  // This IntegrationFlow bean creates a function bean named "publishToGraphEvent" defined by
  // AnalysisPublishToGraphEvent interface for use with the function composition in app
  // properties. It's done this way to use IntegrationFlows filter and fluxTransform because
  // a directly defined Flux to Flux function bean controls full stream behavior including errors,
  // which have to be captured and DLQed manually.
  @Bean
  public IntegrationFlow analysisEventToGraphEventFlow() {
    return IntegrationFlows.from(
            AnalysisEventToGraphEvent.class, gateway -> gateway.beanName("analysisEventToGraphEvent"))
        .transform(AnalysisEvent::getAnalysis)
        .<Analysis>filter(a -> a.getAnalysisState().equalsIgnoreCase(ACCEPTED_ANALYSIS_STATE))
        .<Analysis>filter(a -> a.getAnalysisType().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE))
        .transform(analysisToGraphEvent())
        .logAndReply();
  }

  // Simple function bean that adds GraphEvent+avro contentType for message converter resolution.
  // This is done separately from the IntegrationFlow because that can't resolve message converters.
  @Bean
  public Function<GraphEvent, Message<GraphEvent>> addAvroContentType() {
    return payload ->
        MessageBuilder.withPayload(payload)
            .setHeader("contentType", "application/vnd.GraphEvent+avro")
            .build();
  }

  private GenericTransformer<Analysis, GraphEvent> analysisToGraphEvent() {
    return a -> {
      val donorIds =
          a.getDonors().stream()
              .map(Analysis.AnalysisDonor::getDonorId)
              .collect(toUnmodifiableList());

      val files =
              a.getFiles().stream()
                      .map(f -> new AnalysisFile(f.getDataType()))
                      .collect(toUnmodifiableList());

      String experimentalStrategy = "";
      try {
        val experiment = (Map<String, Object>) a.getExperiment();
        experimentalStrategy = experiment.getOrDefault("experimental_strategy", "").toString();
      } catch (Exception e) {
        log.error("Experiment is not map", e);
      }

      return GraphEvent.newBuilder()
          .setAnalysisId(a.getAnalysisId())
          .setAnalysisState(a.getAnalysisState())
          .setAnalysisType(a.getAnalysisType())
          .setStudyId(a.getStudyId())
          .setDonorIds(donorIds)
          .setFiles(files)
          .setExperimentalStrategy(experimentalStrategy)
          .build();
    };
  }

  public interface AnalysisEventToGraphEvent extends Function<AnalysisEvent, GraphEvent> {}
}
