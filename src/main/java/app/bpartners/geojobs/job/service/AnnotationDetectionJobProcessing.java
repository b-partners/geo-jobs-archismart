package app.bpartners.geojobs.job.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.rest.model.AnnotationJobProcessing;
import app.bpartners.geojobs.endpoint.rest.model.JobType;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnotationDetectionJobProcessing
    implements BiFunction<String, Double, AnnotationJobProcessing> {
  private final EventProducer eventProducer;

  @Override
  public AnnotationJobProcessing apply(String jobId, Double minConfidenceForDelivery) {
    var annotationJobWithoutObjectsId = randomUUID().toString();
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();

    eventProducer.accept(
        List.of(
            AnnotationDeliveryJobRequested.builder()
                .jobId(jobId)
                .minimumConfidenceForDelivery(minConfidenceForDelivery)
                .annotationJobWithObjectsIdTruePositive(annotationJobWithObjectsIdTruePositive)
                .annotationJobWithObjectsIdFalsePositive(annotationJobWithObjectsIdFalsePositive)
                .annotationJobWithoutObjectsId(annotationJobWithoutObjectsId)
                .build()));

    return new AnnotationJobProcessing()
        .jobId(jobId)
        .annotationWithObjectTruePositive(annotationJobWithObjectsIdTruePositive)
        .annotationWithObjectFalsePositive(annotationJobWithObjectsIdFalsePositive)
        .annotationWithoutObjectJobId(annotationJobWithoutObjectsId)
        .jobType(JobType.DETECTION)
        .creationDatetime(Instant.now());
  }
}
