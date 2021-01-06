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

import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc_argo.workflow_graph_lib.schema.AnalysisFile;
import org.icgc_argo.workflow_graph_lib.schema.GraphEvent;
import org.icgc_argo.workflowingestionnode.model.Analysis;
import org.icgc_argo.workflowingestionnode.model.AnalysisEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FunctionDefinitions {
  private static final String ACCEPTED_ANALYSIS_STATE = "PUBLISHED";
  private static final String ACCEPTED_ANALYSIS_TYPE = "sequencing_experiment";

  @Bean
  public Function<Message<AnalysisEvent>, Message<GraphEvent>> processor() {
    return analysisEventMessage -> {
      val analysis = analysisEventMessage.getPayload().getAnalysis();
      log.info("Received analysis: {}", analysis);

      if (!isAcceptedAnalysis(analysis)) {
        log.info("Filtered (ignored) analysis: {}", analysis);
        return null; // returning null acknowledges and discards received message
      }

      val ge = convertToGraphEvent(analysis);
      log.info("Sending graph event: {}", ge);

      return convertToGraphEventMessage(ge);
    };
  }

  private Message<GraphEvent> convertToGraphEventMessage(GraphEvent graphEvent) {
    return MessageBuilder.withPayload(graphEvent)
        .setHeader("contentType", "application/vnd.GraphEvent+avro")
        .build();
  }

  private Boolean isAcceptedAnalysis(Analysis analysis) {
    return analysis.getAnalysisType().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE)
        && analysis.getAnalysisState().equalsIgnoreCase(ACCEPTED_ANALYSIS_STATE);
  }

  private GraphEvent convertToGraphEvent(Analysis analysis) {
    return GraphEvent.newBuilder()
        .setId(UUID.randomUUID().toString())
        .setAnalysisId(analysis.getAnalysisId())
        .setAnalysisState(analysis.getAnalysisState())
        .setAnalysisType(analysis.getAnalysisType())
        .setStudyId(analysis.getStudyId())
        .setDonorIds(analysis.getDonorIds())
        .setFiles(
            analysis.getFiles().stream()
                .map(f -> new AnalysisFile(f.getDataType()))
                .collect(toUnmodifiableList()))
        .setExperimentalStrategy(analysis.getExperiment().getExperimentalStrategy())
        .build();
  }
}
