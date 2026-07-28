package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.file.hash.FileHashAlgorithm.SHA256;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.tile.ParcelTilingTaskCreated;
import app.bpartners.geojobs.endpoint.rest.controller.v1.ZoneTilingController;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.TiledParcel;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.TilingTaskStatusService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import app.bpartners.geojobs.service.tiling.downloader.TilesDownloader;
import app.bpartners.geojobs.sqs.EventProducerInvocationMock;
import app.bpartners.geojobs.sqs.LocalEventQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class ParcelParcelParcelTilingTaskCreatedServiceIT extends FacadeIT {
  public static final String GEOMETRY_MOCK =
      "{\n"
          + "    \"type\": \"MultiPolygon\",\n"
          + "    \"coordinates\": [ [ [\n"
          + "      [ 4.459648282829194, 45.904988912620688 ],\n"
          + "      [ 4.464709510872551, 45.928950368349426 ],\n"
          + "      [ 4.490816965688656, 45.941784543770964 ],\n"
          + "      [ 4.510354299995861, 45.933697132664598 ],\n"
          + "      [ 4.518386257467152, 45.912888345521047 ],\n"
          + "      [ 4.496344031095243, 45.883438201401809 ],\n"
          + "      [ 4.479593950305621, 45.882900828315755 ],\n"
          + "      [ 4.459648282829194, 45.904988912620688 ] ] ] ] }";
  @Autowired ParcelTilingTaskCreatedService subject;
  @Autowired ZoneTilingController zoneTilingController;
  @MockBean BucketComponent bucketComponent;
  @MockBean TilesDownloader tilesDownloader;
  @Autowired TilingTaskRepository tilingTaskRepository;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired ZoneTilingJobService zoneTilingJobService;
  @MockBean EventProducer eventProducer;
  @Autowired TilingTaskStatusService tilingTaskStatusService;
  @Autowired LocalEventQueue localEventQueue;
  EventProducerInvocationMock eventProducerInvocationMock = new EventProducerInvocationMock();
  private app.bpartners.geojobs.repository.model.Feature lyonFeature;

  @BeforeEach
  void setUp() throws JsonProcessingException {
    doAnswer(
            invocationOnMock ->
                eventProducerInvocationMock.apply(localEventQueue, invocationOnMock))
        .when(eventProducer)
        .accept(any());
    when(tilesDownloader.apply(any()))
        .thenAnswer(
            (i) ->
                Paths.get(this.getClass().getClassLoader().getResource("mockData/lyon").toURI())
                    .toFile());
    when(bucketComponent.upload(any(), any())).thenReturn(new FileHash(SHA256, "mock"));
    lyonFeature = defaultFeature();
  }

  private ZoneTilingJob aZTJ(String jobId) {
    return ZoneTilingJob.builder()
        .id(jobId)
        .zoneName("mock")
        .emailReceiver("mock@hotmail.com")
        .build();
  }

  private ParcelTilingTask aZTT(
      String jobId,
      String taskId,
      String parcelId,
      Status.ProgressionStatus progression,
      Status.HealthStatus health) {
    return ParcelTilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(
            List.of(
                Parcel.builder()
                    .id(parcelId)
                    .parcelContent(
                        ParcelContent.builder()
                            .id(randomUUID().toString())
                            .creationDatetime(now())
                            .geoServerParameter(
                                new GeoServerParameter()
                                    .layers("grand-lyon")
                                    .height(1024)
                                    .width(1024))
                            .feature(lyonFeature)
                            .build())
                    .build()))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .progression(progression)
                    .health(health)
                    .build()))
        .build();
  }

  @SneakyThrows
  private ParcelTilingTask aZTT(String jobId, String taskId, String parcelId) {
    return aZTT(jobId, taskId, parcelId, PENDING, UNKNOWN);
  }

  @SneakyThrows
  private ParcelTilingTask aZTT_processing(String jobId, String taskId, String parcelId) {
    return ParcelTilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(
            List.of(
                Parcel.builder()
                    .id(parcelId)
                    .parcelContent(
                        ParcelContent.builder()
                            .id(randomUUID().toString())
                            .geoServerParameter(
                                new GeoServerParameter()
                                    .layers("grand-lyon")
                                    .width(1024)
                                    .height(1024))
                            .feature(defaultFeature())
                            .build())
                    .build()))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(taskId)
                    .progression(PROCESSING)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  public static Feature defaultFeature() {
    return Feature.builder()
        .id("feature_1_id")
        .zoom(10)
        .geometry(
            Feature.FeatureGeometry.builder().actualInstanceStringValue(GEOMETRY_MOCK).build())
        .build();
  }

  @Test
  void unzip_and_upload_ok() {
    String jobId = randomUUID().toString();
    ZoneTilingJob job =
        zoneTilingJobRepository.save(
            ZoneTilingJob.builder()
                .id(jobId)
                .zoneName("mock")
                .emailReceiver("mock@hotmail.com")
                .build());
    String taskId = randomUUID().toString();
    String parcelId = randomUUID().toString();
    ParcelTilingTask toCreate =
        ParcelTilingTask.builder()
            .id(taskId)
            .jobId(job.getId())
            .parcels(
                List.of(
                    Parcel.builder()
                        .id(parcelId)
                        .parcelContent(
                            ParcelContent.builder()
                                .geoServerParameter(
                                    new GeoServerParameter()
                                        .layers("grand-lyon")
                                        .width(1024)
                                        .height(1024))
                                .id(randomUUID().toString())
                                .build())
                        .build()))
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(taskId)
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .build()))
            .build();
    ParcelTilingTask created = tilingTaskRepository.save(toCreate);
    ParcelTilingTaskCreated createdEventPayload = new ParcelTilingTaskCreated(created);
    subject.accept(createdEventPayload);
    int numberOfDirectoryToUpload = 1;
    verify(bucketComponent, times(numberOfDirectoryToUpload)).upload(any(), any(String.class));
  }

  @Test
  void succeed_task_when_consumer_does_not_throws() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    String parcelId = randomUUID().toString();
    ParcelTilingTask toCreate = aZTT(jobId, taskId, parcelId);
    ParcelTilingTask task = tilingTaskRepository.save(toCreate);

    assertDoesNotThrow(() -> subject.accept(new ParcelTilingTaskCreated(task)));

    tilingTaskRepository
        .findById(task.getId())
        .ifPresent(
            tilingTask -> {
              assertTrue(tilingTask.isSucceeded());
            });
  }

  @SneakyThrows
  @Test
  void fail_on_of_several_tasks() {
    var callersNb = 40;
    var tasks = new ArrayList<ParcelTilingTask>();
    var jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    for (int i = 0; i < callersNb; i++) {
      var taskId = randomUUID().toString();
      var parcelId = randomUUID().toString();
      var toCreate = aZTT(jobId, taskId, parcelId, PROCESSING, UNKNOWN);
      tasks.add(tilingTaskRepository.save(toCreate));
    }
    tilingTaskStatusService.fail(tasks.get(7));
    zoneTilingJobService.recomputeStatus(zoneTilingJobRepository.findById(jobId).get());
    var jobStatusAfterFail = zoneTilingJobRepository.findById(jobId).get().getStatus();
    assertEquals(PROCESSING, jobStatusAfterFail.getProgression());
    assertEquals(FAILED, jobStatusAfterFail.getHealth());
    var tasksAfterFail = zoneTilingController.getZTJParcels(jobId);
    assertTrue(
        tasksAfterFail.stream()
            .map(TiledParcel::getStatus)
            .allMatch(
                (status) ->
                    app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.PROCESSING
                                .equals(status.getProgression())
                            && app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.UNKNOWN
                                .equals(status.getHealth())
                        || app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED
                                .equals(status.getProgression())
                            && app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.FAILED
                                .equals(status.getHealth())));
  }

  @Test
  void task_processing_to_pending_ko() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    String parcelId = randomUUID().toString();
    ParcelTilingTask toCreate = aZTT_processing(jobId, taskId, parcelId);
    ParcelTilingTask created = tilingTaskRepository.save(toCreate);
    List<TaskStatus> statuses =
        tilingTaskRepository.findById(created.getId()).orElseThrow().getStatusHistory().stream()
            .toList();
    List<TaskStatus> statusesAfterFailedStatusTransition =
        tilingTaskRepository.findById(created.getId()).orElseThrow().getStatusHistory().stream()
            .toList();
    assertEquals(statusesAfterFailedStatusTransition, statuses);
    assertFalse(statuses.isEmpty());
    assertFalse(statusesAfterFailedStatusTransition.isEmpty());
  }

  @Test
  void get_ztj_tiles_after_successful_first_attempt() {
    String jobId = randomUUID().toString();
    zoneTilingJobRepository.save(aZTJ(jobId));
    String taskId = randomUUID().toString();
    String parcelId = randomUUID().toString();
    ParcelTilingTask toCreate = aZTT(jobId, taskId, parcelId);
    ParcelTilingTask created = tilingTaskRepository.save(toCreate);
    ParcelTilingTaskCreated ztjCreated = new ParcelTilingTaskCreated(created);

    assertDoesNotThrow(() -> subject.accept(ztjCreated));

    zoneTilingJobService.recomputeStatus(zoneTilingJobRepository.findById(jobId).get());
    List<TiledParcel> parcels = zoneTilingController.getZTJParcels(jobId);
    assertEquals(1, parcels.size());
    assertEquals(2, parcels.getFirst().getTiles().size());
  }
}
