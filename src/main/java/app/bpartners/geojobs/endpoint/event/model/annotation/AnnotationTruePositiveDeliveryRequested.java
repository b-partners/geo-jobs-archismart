package app.bpartners.geojobs.endpoint.event.model.annotation;

import static app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationModelDeliveryType.TRUE_POSITIVE;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.List;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString
public class AnnotationTruePositiveDeliveryRequested extends AnnotationModelDeliveryRequested {
  public AnnotationTruePositiveDeliveryRequested(
      String zoneDetectionJobId,
      String annotationJobId,
      Double minimumConfidenceForDelivery,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    super(
        zoneDetectionJobId,
        annotationJobId,
        minimumConfidenceForDelivery,
        detectableObjectConfigurations);
  }

  @Override
  public AnnotationModelDeliveryType getAnnotationModelDeliveryType() {
    return TRUE_POSITIVE;
  }
}
