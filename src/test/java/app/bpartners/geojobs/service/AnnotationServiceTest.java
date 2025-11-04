package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiClient;
import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.annotator.AnnotatorApiConf;
import app.bpartners.geojobs.service.annotator.CreateAnnotationBatchExtractor;
import app.bpartners.geojobs.service.annotator.LabelConverter;
import app.bpartners.geojobs.service.annotator.LabelExtractor;
import app.bpartners.geojobs.service.annotator.PolygonExtractor;
import app.bpartners.geojobs.service.annotator.TaskExtractor;
import java.math.BigDecimal;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

class AnnotationServiceTest {
  public static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  MockedConstruction<JobsApi> jobsApiMockedConstruction;
  MockedConstruction<AdminApi> adminApiMockedConstruction;
  DetectableObjectConfigurationRepository detectableObjectRepositoryMock = mock();
  LabelConverter labelConverterMock = mock();
  AnnotatorApiConf annotatorApiConfMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  AnnotationDeliveryJobService deliverJobServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  LabelExtractor labelExtractor =
      new LabelExtractor(new KeyPredicateFunction(), labelConverterMock);
  CreateAnnotationBatchExtractor batchExtractor =
      new CreateAnnotationBatchExtractor(labelExtractor, new PolygonExtractor());
  TaskExtractor taskExtractor = new TaskExtractor(batchExtractor, labelExtractor);
  AnnotationService subject;

  @NonNull
  private static List<DetectableObjectConfiguration> detectableObjects() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(PASSAGE_PIETON).build(),
        DetectableObjectConfiguration.builder().objectType(TOITURE_REVETEMENT).build());
  }

  @SneakyThrows
  @NonNull
  private static List<MachineDetectedTile> detectedTiles() {
    return List.of(
        MachineDetectedTile.builder().id("detectedTile1Id").detectedObjects(List.of()).build(),
        MachineDetectedTile.builder()
            .id("detectedTile2Id")
            .detectedObjects(
                List.of(
                    DetectedObject.builder()
                        .detectedObjectType(
                            DetectableObjectType.builder().detectableType(PASSAGE_PIETON).build())
                        .feature(
                            app.bpartners.geojobs.repository.model.Feature.builder()
                                .geometry(
                                    app.bpartners.geojobs.repository.model.Feature.FeatureGeometry
                                        .builder()
                                        .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                                        .actualInstanceStringValue(
                                            objectMapper()
                                                .writeValueAsString(
                                                    new MultiPolygon()
                                                        .coordinates(
                                                            List.of(
                                                                List.of(
                                                                    List.of(
                                                                        List.of(
                                                                            new BigDecimal("0.8"),
                                                                            new BigDecimal(
                                                                                "0.9"))))))))
                                        .build())
                                .build())
                        .computedConfidence(0.8)
                        .build()))
            .build());
  }

  @BeforeEach
  void setUp() {
    jobsApiMockedConstruction = mockConstruction(JobsApi.class);
    adminApiMockedConstruction = mockConstruction(AdminApi.class);
    subject =
        new AnnotationService(
            annotatorApiConfMock,
            taskExtractor,
            labelConverterMock,
            labelExtractor,
            mock(),
            deliverJobServiceMock,
            objectConfigurationRepositoryMock,
            bucketComponentMock);

    when(detectableObjectRepositoryMock.findAllByDetectionJobId(ZONE_DETECTION_JOB_ID))
        .thenReturn(detectableObjects());
    when(labelConverterMock.apply(PASSAGE_PIETON))
        .thenReturn(new Label().name(PASSAGE_PIETON.name()));
    when(annotatorApiConfMock.newApiClientWithApiKey()).thenReturn(new ApiClient());
  }

  @AfterEach
  void tearDown() {
    jobsApiMockedConstruction.close();
    adminApiMockedConstruction.close();
  }

  @SneakyThrows
  @Test
  void createAnnotationJob_with_some_un_found_objects() {
    var jobsApi = jobsApiMockedConstruction.constructed().getFirst();
    when(jobsApi.saveJob(any(), any())).thenReturn(new Job().id("annotatorJobId"));
    var annotationDeliveryJobCapture = ArgumentCaptor.forClass(AnnotationDeliveryJob.class);
    var annotationDeliveryTasksCapture = ArgumentCaptor.forClass(List.class);

    subject.createAnnotationJob(humanDetectionJob());

    verify(deliverJobServiceMock, only())
        .create(annotationDeliveryJobCapture.capture(), annotationDeliveryTasksCapture.capture());

    var annotationDeliveryJobValue = annotationDeliveryJobCapture.getValue();
    var annotationDeliveryTaskValues = annotationDeliveryJobCapture.getAllValues();

    assertEquals(
        humanDetectionJob().getAnnotationJobId(), annotationDeliveryJobValue.getAnnotationJobId());
    assertFalse(annotationDeliveryTaskValues.isEmpty());
  }

  private HumanDetectionJob humanDetectionJob() {
    return HumanDetectionJob.builder()
        .id("humanDetectionJob")
        .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
        .annotationJobId("annotationJobId")
        .machineDetectedTiles(detectedTiles())
        .detectableObjectConfigurations(detectableObjects())
        .build();
  }
}
