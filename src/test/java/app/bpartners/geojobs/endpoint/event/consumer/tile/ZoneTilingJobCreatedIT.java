package app.bpartners.geojobs.endpoint.event.consumer.tile;

import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.HumanZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobCreated;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.AnnotationDeliveryConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryConfiguration;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import app.bpartners.geojobs.utils.detection.DetectionIT;
import app.bpartners.geojobs.utils.detection.ObjectsDetectorMockResponse;
import app.bpartners.geojobs.utils.tiling.TilingTaskCreator;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
@Disabled("flaky test on CI : flyway launches before postgres")
class ZoneTilingJobCreatedIT extends DetectionIT {
  private static final double OBJECT_DETECTION_SUCCESS_RATE = 100.0;
  private static final int DEFAULT_EVENT_DELAY_SPEED_FACTOR = 10;
  private static final double MOCK_DETECTION_RESPONSE_CONFIDENCE = 0.9;
  private static final String ANNOTATION_JOB_ID = "annotationJobId";
  @Autowired DetectionRepository detectionRepository;
  @Autowired TilingTaskRepository tilingTaskRepository;
  @Autowired LocalEventQueue localEventQueue;
  @MockBean EventProducer eventProducerMock;
  @MockBean TilesDownloader tilesDownloaderMock;
  @MockBean BucketComponent bucketComponentMock;
  @MockBean JobFinishedMailer<ZoneTilingJob> tilingJobMailerMock;
  @MockBean AnnotationService annotationServiceMock;
  @MockBean GeoJsonConversionJobService geoJsonConversionJobServiceMock;
  @MockBean AnnotationDeliveryConfigurationRepository annotationDeliveryConfigurationRepositoryMock;
  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();
  TilingTaskCreator tilingTaskCreator = new TilingTaskCreator();

  @BeforeEach
  void setUp() {
    when(annotationDeliveryConfigurationRepositoryMock.findLatestConfiguration())
        .thenReturn(
            Optional.of(
                AnnotationDeliveryConfiguration.builder()
                    .minimumConfidenceForDelivery(0.0)
                    .build()));
    localEventQueue.configure(customEventConfigList(), DEFAULT_EVENT_DELAY_SPEED_FACTOR);
    doAnswer(
            invocationOnMock ->
                eventProducerInvocationMock.apply(localEventQueue, invocationOnMock))
        .when(eventProducerMock)
        .accept(any());
    when(jobAnnotationServiceMock.processAnnotationJob(any(), any())).thenReturn(null);
    doNothing().when(mailerMock).accept(any());
    doNothing().when(tilingJobMailerMock).accept(any());
    new ObjectsDetectorMockResponse(objectsDetectorMock)
        .apply(MOCK_DETECTION_RESPONSE_CONFIDENCE, "PISCINE", OBJECT_DETECTION_SUCCESS_RATE);
    when(tilesDownloaderMock.apply(any()))
        .thenAnswer(
            (i) ->
                Paths.get(this.getClass().getClassLoader().getResource("mockData/lyon").toURI())
                    .toFile());
    when(bucketComponentMock.upload(any(), any())).thenReturn(new FileHash(SHA256, "mock"));

    doNothing().when(annotationServiceMock).createAnnotationJob(any(), any(), any());
    when(annotationServiceMock.getAnnotationJobById(any())).thenReturn(annotationJob());
    when(annotationServiceMock.retrieveTasksFromAnnotationJob(
            any(), any(), eq(ANNOTATION_JOB_ID), eq(null), eq(null), eq(null)))
        .thenAnswer(
            invocationOnMock ->
                retrievingTasks(
                    invocationOnMock.getArgument(0),
                    invocationOnMock.getArgument(1),
                    ANNOTATION_JOB_ID));
    when(annotationServiceMock.getAnnotations(eq(ANNOTATION_JOB_ID), any()))
        .thenReturn(getAnnotations());
  }

  @NonNull
  private static List<AnnotationBatch> getAnnotations() {
    return List.of(
        new AnnotationBatch()
            .annotations(
                List.of(
                    new Annotation()
                        .polygon(new Polygon().points(List.of(new Point().x(100.0).y(200.0))))
                        .comment("confidence=90.0")
                        .label(new Label().name("PISCINE")))));
  }

  @NonNull
  private List<AnnotationRetrievingTask> retrievingTasks(
      String humanDetectionJobId, String retrievingJobId, String annotationJobId) {
    return List.of(
        AnnotationRetrievingTask.builder()
            .id(randomUUID().toString())
            .jobId(retrievingJobId)
            .annotationJobId(annotationJobId)
            .xTile(100)
            .yTile(200)
            .zoom(20)
            .humanZoneDetectionJobId(humanDetectionJobId) // ignored here
            .annotationJobId(annotationJobId)
            .statusHistory(new ArrayList<>())
            .submissionInstant(now())
            .build());
  }

  private Job annotationJob() {
    return new Job().id(ANNOTATION_JOB_ID).status(JobStatus.COMPLETED);
  }

  @NonNull
  private static List<LocalEventQueue.CustomEventDelayConfig> customEventConfigList() {
    return List.of(
        new LocalEventQueue.CustomEventDelayConfig(AutoTaskStatisticRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(HumanZDJStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(
            AnnotationRetrievingJobStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(
            ParcelDetectionStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZTJStatusRecomputingSubmitted.class, 50),
        new LocalEventQueue.CustomEventDelayConfig(ZDJStatusRecomputingSubmitted.class, 50));
  }

  @SneakyThrows
  @Test
  @Disabled
  void ztj_created_and_ztj_and_zdj_succeed() {
    var tilingJob = zoneTilingJobId();

    eventProducerMock.accept(
        List.of(ZoneTilingJobCreated.builder().zoneTilingJob(tilingJob).build()));

    Thread.sleep(Duration.ofSeconds(90L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();

    var actualTilingJob = ztjRepository.findById(tilingJob.getId()).orElseThrow();
    var actualDetectionJob = zdjService.getByTilingJobId(tilingJob.getId(), MACHINE);
    assertTrue(actualTilingJob.isSucceeded());
    assertTrue(actualDetectionJob.isSucceeded());
  }

  @Test
  @SneakyThrows
  @Disabled
  void ztj_created_and_zdj_machine_succeeded_and_zdj_human_succeeded_ok() {
    var tilingJob = zoneTilingJobId();

    eventProducerMock.accept(
        List.of(ZoneTilingJobCreated.builder().zoneTilingJob(tilingJob).build()));

    Thread.sleep(Duration.ofSeconds(30L));
    var actualTilingJob = ztjRepository.findById(tilingJob.getId()).orElseThrow();
    var actualDetectionJob = zdjService.getByTilingJobId(tilingJob.getId(), MACHINE);
    assertTrue(actualTilingJob.isSucceeded());
    assertTrue(actualDetectionJob.isSucceeded());

    verify(annotationServiceMock, times(1)).createAnnotationJob(any(), any(), any());
    var defaultDetectionJobHuman = zdjService.getByTilingJobId(tilingJob.getId(), HUMAN);
    eventProducerMock.accept(
        List.of(
            AnnotationJobVerificationSent.builder()
                .humanZdjId(defaultDetectionJobHuman.getId())
                .build()));

    Thread.sleep(Duration.ofSeconds(60L));
    if (localEventQueue != null) localEventQueue.attemptSchedulerShutDown();
    var actualDetectionJobHuman = zdjService.getByTilingJobId(tilingJob.getId(), HUMAN);

    verify(geoJsonConversionJobServiceMock, times(1))
        .getOrComputeGeoJsonUrl(actualDetectionJobHuman.getId());

    assertTrue(actualDetectionJobHuman.isSucceeded());
  }

  @NonNull
  private ZoneTilingJob zoneTilingJobId() {
    var detectionId = randomUUID().toString();
    var endToEndId = randomUUID().toString();
    var tilingJobId = randomUUID().toString();
    var tilingJob =
        ztjRepository.save(
            zoneTilingJobCreator.create(
                tilingJobId, "dummyZoneName", "dummy@email.com", PENDING, UNKNOWN));
    var parcel = parcelRepository.save(parcelCreator.create(1));
    var tilingTask =
        tilingTaskCreator.create(
            randomUUID().toString(), tilingJob.getId(), parcel, PENDING, UNKNOWN);
    tilingTaskRepository.save(tilingTask);
    detectionRepository.save(
        Detection.builder()
            .id(detectionId)
            .endToEndId(endToEndId)
            .ztjId(tilingJobId)
            .detectableObjectConfigurations(
                List.of(
                    DetectableObjectConfiguration.builder()
                        .bucketStorageName(null)
                        .objectType(PISCINE)
                        .minConfidenceForDetection(1.0)
                        .build()))
            .build());
    return tilingJob;
  }
}
