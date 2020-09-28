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

import java.util.function.Function;

import org.icgc_argo.workflowingestionnode.model.AnalysisPublishEvent;
import org.icgc_argo.workflowingestionnode.model.NodeEvent;
import org.icgc_argo.workflowingestionnode.rdpc.RdpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class FunctionDefinitions {
  private static final String ACCEPTED_ANALYSIS_TYPE = "sequencing_experiment";

  private final RdpcService rdpcService;

  @Autowired
  public FunctionDefinitions(RdpcService rdpcService) {
    this.rdpcService = rdpcService;
  }

  @Bean
  public Function<Flux<AnalysisPublishEvent>, Flux<NodeEvent>> processor() {
    return analysisPublishEventFlux ->
        analysisPublishEventFlux
            .filter(
                analysisPublishEvent ->
                    analysisPublishEvent.getAnalysisType().equalsIgnoreCase(ACCEPTED_ANALYSIS_TYPE))
            .flatMap(
                analysisPublishEvent ->
                    rdpcService.createNodeEventFromAnalysis(analysisPublishEvent.getAnalysisId()))
            .doOnError(Throwable::printStackTrace)
            .log();
  }
}
