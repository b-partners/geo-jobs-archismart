package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.endpoint.event.model.status.JobStatusRecomputingSubmitted;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class DetectionAddressConversionJobStatusRecomputingSubmitted
    extends JobStatusRecomputingSubmitted {
  private static final long INITIAL_BACKOFF_DURATION_IN_SECONDS =
      Duration.ofSeconds(30L).getSeconds();

  public DetectionAddressConversionJobStatusRecomputingSubmitted(String jobId) {
    super(jobId, INITIAL_BACKOFF_DURATION_IN_SECONDS);
  }
}
