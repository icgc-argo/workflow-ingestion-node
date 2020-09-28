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

import static java.lang.String.format;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.OkHttpClient;
import org.icgc_argo.workflowingestionnode.graphql.client.GetAnalysisDetailsQuery;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Slf4j
@Component
public class RdpcClient {

  /** State */
  private final ApolloClient client;

  @Autowired
  public RdpcClient(@NonNull RdpcConfig rdpcConfig) {
    val okHttpBuilder = new OkHttpClient.Builder();
    okHttpBuilder.connectTimeout(60, TimeUnit.SECONDS);
    okHttpBuilder.callTimeout(60, TimeUnit.SECONDS);
    okHttpBuilder.readTimeout(60, TimeUnit.SECONDS);
    okHttpBuilder.writeTimeout(60, TimeUnit.SECONDS);

    this.client =
        ApolloClient.builder()
            .serverUrl(rdpcConfig.getRdpcUrl())
            .okHttpClient(okHttpBuilder.build())
            .build();
  }

  /**
   * Get the Analysis details for a NodeEvent creation
   *
   * @param analysisId The runId of the workflow as a String
   * @return Returns a Mono of the analysis details for NodeEvent creation
   */
  public Mono<GetAnalysisDetailsQuery.Analysis> getAnalysisDetailsForNodeEvent(String analysisId) {
    return Mono.create(
        sink ->
            client
                .query(new GetAnalysisDetailsQuery(analysisId))
                .enqueue(
                    new ApolloCall.Callback<>() {
                      final Runnable commonSinkErrorHandler =
                          () ->
                              sinkError(
                                  sink,
                                  format(
                                      "Analysis %s is in compatible with schema Analysis!",
                                      analysisId));

                      @Override
                      public void onResponse(
                          @NotNull Response<Optional<GetAnalysisDetailsQuery.Data>> response) {
                        Objects.requireNonNull(
                                response.getData(), format("Response for %s is null!", analysisId))
                            .ifPresentOrElse(
                                data ->
                                    data.getAnalyses()
                                        .ifPresentOrElse(
                                            analyses ->
                                                analyses.stream()
                                                    .findFirst()
                                                    .ifPresentOrElse(
                                                        sink::success, commonSinkErrorHandler),
                                            commonSinkErrorHandler),
                                commonSinkErrorHandler);
                      }

                      @Override
                      public void onFailure(@NotNull ApolloException e) {
                        sink.error(e);
                      }

                      private void sinkError(MonoSink<?> sink, String message) {
                        log.error(message);
                        sink.error(new RuntimeException(message));
                      }
                    }));
  }
}
