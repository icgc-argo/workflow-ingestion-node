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

  // Simple function bean that adds avro GraphEvent contentType so correct message converter bean is
  // used
  @Bean
  public Function<Object, Message<Object>> addAvroContentType() {
    return payload ->
        MessageBuilder.withPayload(payload)
            .setHeader("contentType", "application/vnd.GraphEvent+avro")
            .build();
  }

  // This IntegrationFlow wraps the Flux to Flux function with spring integration, which creates
  // a "publishToGraphEvent" function bean for use with the function composition in app properties
  // The main reason it done this way is because Flux to Flux function bean controls full stream
  // behavior including error handling, which means errors have
  // to be captured and DLQed manually or stream will terminate (but app keeps running!)
  @Bean
  public IntegrationFlow publishToGraphEventFlow() {
    return IntegrationFlows.from(
            AnalysisPublishToGraphEvent.class, gateway -> gateway.beanName("publishToGraphEvent"))
        .fluxTransform(publishToGraphEvent())
        .<GraphEvent>filter(
            graphEvent -> graphEvent.getAnalysisType().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE))
        .logAndReply();
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
                        // retry with backoff to minimize race condition between publish event and
                        // es indexing
                        .retryWhen(Retry.backoff(15, Duration.ofSeconds(3))))
            .doOnNext(graphEvent -> log.info("Converted to graph event: " + graphEvent))
            .log();
  }

  public interface AnalysisPublishToGraphEvent
      extends Function<Message<AnalysisPublishEvent>, GraphEvent> {}
}
