package app.bpartners.geojobs.endpoint.event.model.zone;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class ImportedZoneTilingJobSaved extends PojaEvent {
  private Long startFrom;
  private Long endAt;
  private String jobId;
  private String bucketName;
  private String bucketPathPrefix;
  private GeoServerParameter geoServerParameter;
  private String geoServerUrl;
  private BucketSeparatorType bucketSeparatorType;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
