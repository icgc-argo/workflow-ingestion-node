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

import java.time.Duration;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.icgc_argo.workflow_graph_lib.schema.GraphEvent;
import org.icgc_argo.workflow_graph_lib.workflow.client.RdpcClient;
import org.icgc_argo.workflowingestionnode.model.AnalysisPublishEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class FunctionDefinitions {
  private static final String ACCEPTED_ANALYSIS_TYPE = "sequencing_experiment";

  private RdpcClient rdpcClient;

  @Autowired
  public FunctionDefinitions(RdpcClient rdpcClient) {
    this.rdpcClient = rdpcClient;
  }

  // IntegrationFlow bean creates a Flux to Flux (defined by AnalysisPublishToGraphEvent interface)
  // function bean named "publishToGraphEvent" for use with the function composition in app
  // properties. It's done this way because a directly defined Flux to Flux function bean controls
  // full stream behavior including errors, which have to be captured and DLQed manually.
  @Bean
  public IntegrationFlow publishToGraphEventFlow() {
    return IntegrationFlows.from(
            AnalysisPublishToGraphEvent.class, gateway -> gateway.beanName("publishToGraphEvent"))
        .fluxTransform(publishToGraphEvent())
        .<GraphEvent>filter(ge -> ge.getAnalysisType().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE))
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

  private Function<Flux<Message<AnalysisPublishEvent>>, Flux<GraphEvent>> publishToGraphEvent() {
    // TODO - in #12 transfrom analysis in event directly to GraphEvent
    return analysisPublishEventFlux ->
        analysisPublishEventFlux
            .map(Message::getPayload)
            .doOnNext(
                analysisPublishEvent -> log.info("Received publish event: " + analysisPublishEvent))
            .flatMap(
                analysisPublishEvent ->
                    rdpcClient
                        .createGraphEventForAnalysis(analysisPublishEvent.getAnalysisId())
                        // retry+backoff to minimize race condition between event and indexing
                        .retryWhen(Retry.backoff(15, Duration.ofSeconds(3)))
                        .log())
            .doOnNext(graphEvent -> log.info("Converted to graph event: " + graphEvent))
            .log();
  }

  public interface AnalysisPublishToGraphEvent
      extends Function<Message<AnalysisPublishEvent>, GraphEvent> {}
}
