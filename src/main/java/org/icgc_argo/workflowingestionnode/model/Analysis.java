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

package org.icgc_argo.workflowingestionnode.model;

import static java.util.stream.Collectors.toUnmodifiableList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Analysis {
  private String analysisId;
  private AnalysisType analysisType;
  private String analysisState;
  private String studyId;
  private List<AnalysisSample> analysisSamples;
  private List<AnalysisFile> files;
  private AnalysisExperiment experiment;

  public String getAnalysisType() {
    return analysisType.getName();
  }

  public List<String> getDonorIds() {
    return analysisSamples.stream()
        .flatMap(
            analysisSample ->
                analysisSample.getAnalysisSpecimens().stream()
                    .flatMap(
                        specimen -> specimen.getAnalysisDonors().stream().map(AnalysisDonor::getDonorId)))
        .distinct()
        .collect(toUnmodifiableList());
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AnalysisType {
    private String name;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AnalysisSample {
    private List<AnalysisSpecimen> analysisSpecimens;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AnalysisSpecimen {
    private List<AnalysisDonor> analysisDonors;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AnalysisDonor {
    private String donorId;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AnalysisFile {
    private String dataType;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
  public static class AnalysisExperiment {
    private String experimentalStrategy;
  }
}
