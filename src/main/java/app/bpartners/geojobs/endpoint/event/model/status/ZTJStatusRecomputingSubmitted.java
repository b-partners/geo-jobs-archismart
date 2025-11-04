package app.bpartners.geojobs.endpoint.event.model.status;

import java.time.Duration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class ZTJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private static final long INITIAL_BACKOFF_IN_SECONDS = Duration.ofSeconds(30).toSeconds();

  public ZTJStatusRecomputingSubmitted(String jobId) {
    super(jobId, INITIAL_BACKOFF_IN_SECONDS);
  }

  public ZTJStatusRecomputingSubmitted(
      String jobId, Long maxConsumerBackoffBetweenRetriesDurationValue) {
    super(
        jobId,
        maxConsumerBackoffBetweenRetriesDurationValue == null
            ? INITIAL_BACKOFF_IN_SECONDS
            : maxConsumerBackoffBetweenRetriesDurationValue);
  }
}
