package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationFalsePositiveDeliveryRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTruePositiveDeliveryRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationWithoutDetectedObjectDeliveryRequested;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationDeliveryJobRequestedService
    implements Consumer<AnnotationDeliveryJobRequested> {
  private final EventProducer eventProducer;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(AnnotationDeliveryJobRequested annotationDeliveryJobRequested) {
    var zoneDetectionJobId = annotationDeliveryJobRequested.getJobId();
    var annotationTruePositiveJobId =
        annotationDeliveryJobRequested.getAnnotationJobWithObjectsIdTruePositive();
    var annotationFalsePositiveJobId =
        annotationDeliveryJobRequested.getAnnotationJobWithObjectsIdFalsePositive();
    var annotationWithoutDetectedObjetsJobId =
        annotationDeliveryJobRequested.getAnnotationJobWithoutObjectsId();
    var minimumConfidenceForDelivery =
        annotationDeliveryJobRequested.getMinimumConfidenceForDelivery();
    var detectableObjectConfigurations = getDetectableObjectConfigurations(zoneDetectionJobId);

    eventProducer.accept(
        List.of(
            new AnnotationTruePositiveDeliveryRequested(
                zoneDetectionJobId,
                annotationTruePositiveJobId,
                minimumConfidenceForDelivery,
                detectableObjectConfigurations)));
    eventProducer.accept(
        List.of(
            new AnnotationFalsePositiveDeliveryRequested(
                zoneDetectionJobId,
                annotationFalsePositiveJobId,
                minimumConfidenceForDelivery,
                detectableObjectConfigurations)));
    eventProducer.accept(
        List.of(
            new AnnotationWithoutDetectedObjectDeliveryRequested(
                zoneDetectionJobId,
                annotationWithoutDetectedObjetsJobId,
                minimumConfidenceForDelivery,
                detectableObjectConfigurations)));
  }

  private List<DetectableObjectConfiguration> getDetectableObjectConfigurations(String jobId) {
    var persistedObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
    var optionalDetection = detectionRepository.findByZdjId(jobId);
    return persistedObjectConfigurations.isEmpty()
        ? (optionalDetection.map(Detection::getDetectableObjectConfigurations).orElseGet(List::of))
        : persistedObjectConfigurations;
  }
}
