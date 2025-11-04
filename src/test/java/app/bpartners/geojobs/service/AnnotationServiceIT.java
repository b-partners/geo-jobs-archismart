package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Disabled(
    "warm annotator db then change geo-jobs teamId and change annotator-api-key before running")
@Slf4j
public class AnnotationServiceIT extends FacadeIT {
  @Autowired AnnotationService annotationService;
  @MockBean EventProducer eventProducerMock;
  private static final Instant NOW = Instant.now();
  private static final String CURRENT_ANNOTATION_JOB_ID = randomUUID().toString();

  @Test
  void create_job_ok() throws ApiException {
    var inDoubtTiles =
        List.of(
            inDoubtTile(null, null, null, null, DetectableType.PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, DetectableType.TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, DetectableType.PISCINE),
            inDoubtTile(null, null, null, null, DetectableType.TOITURE_REVETEMENT),
            inDoubtTile(null, null, null, null, DetectableType.PASSAGE_PIETON),
            inDoubtTile(null, null, null, null, DetectableType.TOITURE_REVETEMENT));

    annotationService.createAnnotationJob(
        HumanDetectionJob.builder()
            .annotationJobId(CURRENT_ANNOTATION_JOB_ID)
            .machineDetectedTiles(inDoubtTiles)
            .build());
    Job createdJob = annotationService.getAnnotationJobById(CURRENT_ANNOTATION_JOB_ID);

    assertNotNull(createdJob);
    log.info("created Job with ID = {}", createdJob.getId());
    verify(eventProducerMock, times(inDoubtTiles.size())).accept(anyList());
  }

  @SneakyThrows
  public static MachineDetectedTile inDoubtTile(
      String jobId,
      String tileId,
      String parcelId,
      String detectedObjectId,
      DetectableType detectableType) {
    BigDecimal value = BigDecimal.valueOf(1.2);
    List<BigDecimal> aPoint = List.of(value, value);
    List<List<BigDecimal>> aPolygon = List.of(aPoint, aPoint);
    List<List<List<BigDecimal>>> aMultiPolygon = List.of(aPolygon);
    MultiPolygon geometry = new MultiPolygon().coordinates(List.of(aMultiPolygon));
    HashMap<String, Object> properties = new HashMap<>();
    var featureId = "featureId";
    properties.put("id", featureId);
    return MachineDetectedTile.builder()
        .id(tileId)
        .zdjJobId(jobId)
        .parcelJobId(null)
        .parcelId(parcelId)
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .id(detectedObjectId)
                    .computedConfidence(0.0)
                    .detectedTileId(tileId)
                    .feature(
                        Feature.builder()
                            .id(featureId)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .actualInstanceStringValue(
                                        objectMapper().writeValueAsString(geometry))
                                    .build())
                            .properties(properties)
                            .build())
                    .detectedObjectType(
                        DetectableObjectType.builder()
                            .id(detectedObjectId)
                            .detectableType(detectableType)
                            .objectId(detectedObjectId)
                            .build())
                    .build()))
        .bucketPath(null)
        .creationDatetime(NOW)
        .tile(new Tile())
        .build();
  }
}
