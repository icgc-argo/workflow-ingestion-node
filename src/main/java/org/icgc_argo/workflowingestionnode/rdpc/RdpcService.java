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

package org.icgc_argo.workflowingestionnode.rdpc;

import static java.util.stream.Collectors.toList;

import java.util.Map;
import lombok.val;
import org.icgc_argo.workflowingestionnode.model.NodeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RdpcService {
  private final RdpcClient rdpcClient;

  @Autowired
  public RdpcService(RdpcClient rdpcClient) {
    this.rdpcClient = rdpcClient;
  }

  public Mono<NodeEvent> createNodeEventFromAnalysis(String analysisId) {
    val analysisMono = rdpcClient.getAnalysisDetailsForNodeEvent(analysisId);

    return analysisMono
        .map(
            analysis -> {
              val id = analysis.getAnalysisId();
              val studyId = analysis.getStudyId().orElseThrow();
              val donorIds =
                  analysis.getDonors().orElseThrow().stream()
                      .map(d -> d.getDonorId().orElseThrow())
                      .collect(toList());
              val experiment = (Map<String, Object>) analysis.getExperiment().orElseThrow();
              val strategy = experiment.get("experimental_strategy").toString();
              return new NodeEvent(id, studyId, strategy, donorIds);
            })
        .doOnError(Throwable::printStackTrace)
        .onErrorStop();
  }
}
