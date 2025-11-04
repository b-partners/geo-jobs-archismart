package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnnotationDeliveryJobRequestedServiceTest {
  private static final double MINIMUM_CONFIDENCE_FOR_DELIVERY = 0.95;
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotationDeliveryJobRequestedService subject =
      new AnnotationDeliveryJobRequestedService(
          eventProducerMock, objectConfigurationRepositoryMock, detectionRepositoryMock);

  @Test
  void accept_with_persisted_object_conf_ok() {
    List<DetectableObjectConfiguration> objectConfigurations =
        List.of(new DetectableObjectConfiguration());
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(any()))
        .thenReturn(objectConfigurations);
    String jobId = "jobId";
    double minConfidence = 0.8;
    String truePositive = "truePositive";
    String falsePositive = "falsePositive";
    String withoutObjects = "withoutObjects";

    subject.accept(
        AnnotationDeliveryJobRequested.builder()
            .jobId(jobId)
            .minimumConfidenceForDelivery(minConfidence)
            .annotationJobWithObjectsIdTruePositive(truePositive)
            .annotationJobWithObjectsIdFalsePositive(falsePositive)
            .annotationJobWithoutObjectsId(withoutObjects)
            .build());

    verify(eventProducerMock, times(3)).accept(any());
  }

  @Test
  void accept_with_detection_object_conf_ok() {
    List<DetectableObjectConfiguration> objectConfigurations =
        List.of(new DetectableObjectConfiguration());
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(any())).thenReturn(List.of());
    when(detectionRepositoryMock.findByZdjId(any()))
        .thenReturn(
            Optional.of(
                Detection.builder().detectableObjectConfigurations(objectConfigurations).build()));

    String jobId = "jobId";
    String truePositive = "truePositive";
    String falsePositive = "falsePositive";
    String withoutObjects = "withoutObjects";

    subject.accept(
        AnnotationDeliveryJobRequested.builder()
            .jobId(jobId)
            .minimumConfidenceForDelivery(MINIMUM_CONFIDENCE_FOR_DELIVERY)
            .annotationJobWithObjectsIdTruePositive(truePositive)
            .annotationJobWithObjectsIdFalsePositive(falsePositive)
            .annotationJobWithoutObjectsId(withoutObjects)
            .build());

    verify(eventProducerMock, times(3)).accept(any());
  }
}
