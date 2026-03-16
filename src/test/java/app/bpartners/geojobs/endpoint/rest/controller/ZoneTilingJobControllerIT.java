package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob.ZoomLevelEnum.TOWN;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
class ZoneTilingJobControllerIT extends FacadeIT {
  private final String jobId;
  private final String tilingTask1Id;
  private final String tilingTask2Id;
  @Autowired ZoneTilingController controller;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired TilingTaskRepository taskRepository;
  @MockBean EventProducer eventProducer;
  @Autowired ObjectMapper om;
  @Autowired ZoneTilingJobMapper tilingJobMapper;

  ZoneTilingJobControllerIT() {
    this.jobId = randomUUID().toString();
    this.tilingTask1Id = randomUUID().toString();
    this.tilingTask2Id = randomUUID().toString();
  }

  @SneakyThrows
  @BeforeEach
  void setUp() {
    zoneTilingJobRepository.save(
        ZoneTilingJob.builder()
            .id(jobId)
            .emailReceiver("dummy@email.com")
            .zoneName("dummyZoneName")
            .build());
    ParcelTilingTask taskWithoutParcel =
        ParcelTilingTask.builder()
            .id(tilingTask1Id)
            .jobId(jobId)
            .parcels(List.of())
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(tilingTask2Id)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .build();
    ParcelTilingTask taskWithParcel =
        ParcelTilingTask.builder()
            .id(tilingTask2Id)
            .jobId(jobId)
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id(randomUUID().toString())
                        .taskId(tilingTask2Id)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .parcels(
                List.of(
                    Parcel.builder()
                        .id(randomUUID().toString())
                        .parcelContent(
                            ParcelContent.builder()
                                .id(randomUUID().toString())
                                .geoServerUrl(
                                    new URI("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
                                        .toURL())
                                .tiles(List.of(new Tile()))
                                .build())
                        .build()))
            .build();
    taskRepository.saveAll(List.of(taskWithoutParcel, taskWithParcel));
  }

  @AfterEach
  void tearDown() {
    taskRepository.deleteAllById(List.of(tilingTask1Id, tilingTask2Id));
    zoneTilingJobRepository.deleteById(jobId);
  }

  @Test
  void duplicate_tiling_job_ok() {
    var ztj = zoneTilingJobRepository.getById(jobId);

    var actual = controller.duplicateTilingJob(jobId);

    var restExpectedJob =
        tilingJobMapper.toRest(ztj.toBuilder().id(actual.getId()).build(), List.of(), true);
    var expectedJob =
        restExpectedJob
            .creationDatetime(actual.getCreationDatetime())
            .status(
                restExpectedJob
                    .getStatus()
                    .creationDatetime(actual.getStatus().getCreationDatetime()));
    assertEquals(expectedJob.id(actual.getId()), actual);
  }

  CreateZoneTilingJob creatableJob() throws JsonProcessingException {
    return new CreateZoneTilingJob()
        .emailReceiver("mock@hotmail.com")
        .zoneName("Lyon")
        .geoServerUrl("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
        .zoomLevel(TOWN)
        .geoServerParameter(
            om.readValue(
                "{\n"
                    + "    \"service\": \"WMS\",\n"
                    + "    \"request\": \"GetMap\",\n"
                    + "    \"layers\": \"grandlyon:ortho_2018\",\n"
                    + "    \"styles\": \"\",\n"
                    + "    \"format\": \"image/png\",\n"
                    + "    \"transparent\": true,\n"
                    + "    \"version\": \"1.3.0\",\n"
                    + "    \"width\": 256,\n"
                    + "    \"height\": 256,\n"
                    + "    \"srs\": \"EPSG:3857\"\n"
                    + "  }",
                GeoServerParameter.class))
        .features(
            List.of(
                om.readValue(
                    "{ \"type\": \"Feature\",\n"
                        + "  \"properties\": {\n"
                        + "    \"code\": \"69\",\n"
                        + "    \"nom\": \"Rh\u00f4ne\",\n"
                        + "    \"id\": \"feature_1_id\",\n"
                        + "    \"CLUSTER_ID\": 99520,\n"
                        + "    \"CLUSTER_SIZE\": 386884 },\n"
                        + "  \"geometry\": {\n"
                        + "    \"type\": \"MultiPolygon\",\n"
                        + "    \"coordinates\": [ [ [\n"
                        + "      [ 4.459648282829194, 45.904988912620688 ],\n"
                        + "      [ 4.464709510872551, 45.928950368349426 ],\n"
                        + "      [ 4.490816965688656, 45.941784543770964 ],\n"
                        + "      [ 4.510354299995861, 45.933697132664598 ],\n"
                        + "      [ 4.518386257467152, 45.912888345521047 ],\n"
                        + "      [ 4.496344031095243, 45.883438201401809 ],\n"
                        + "      [ 4.479593950305621, 45.882900828315755 ],\n"
                        + "      [ 4.459648282829194, 45.904988912620688 ] ] ] ] } }",
                    Feature.class)));
  }

  @Test
  void create_tiling_job_ok() throws IOException {
    var created = controller.tileZone(creatableJob());

    var actualJobs = controller.getTilingJobs(new PageFromOne(1), new BoundedPageSize(30));
    assertNotNull(created.getId());
    assertTrue(actualJobs.contains(created));
    verify(eventProducer, times(2)).accept(any());
  }

  @Test
  void read_parcels_right_after_job_creation() throws JsonProcessingException {
    var createdJob = controller.tileZone(creatableJob());
    var parcels = controller.getZTJParcels(createdJob.getId());
    var parcel = parcels.getFirst();
    assertEquals(ZoomLevelEnum.TOWN, createdJob.getZoomLevel());
    assertEquals(16, parcels.size());
    assertNotNull(parcel.getId());
    assertNotNull(parcel.getCreationDatetime());
    assertNotNull(parcel.getFeature());
    assertNotNull(parcel.getTiles());
    assertEquals(14, parcel.getFeature().getProperties().get("zoom"));
  }

  @Autowired ZoneTilingJobRepository tilingJobRepository;

  @Test
  void read_parcel_with_non_emptyTiles() {
    var jobId1 = randomUUID().toString();
    var jobId2 = randomUUID().toString();
    var job1 = aZTJ(jobId1);
    var job2 = aZTJ(jobId2);
    var task1 =
        aTask(jobId1, randomUUID().toString(), randomUUID().toString(), randomUUID().toString());
    var task2 =
        aTask(jobId2, randomUUID().toString(), randomUUID().toString(), randomUUID().toString());
    tilingJobRepository.saveAll(List.of(job1, job2));
    taskRepository.saveAll(List.of(task1, task2));

    var parcels1 = controller.getZTJParcels(jobId1);
    var parcels2 = controller.getZTJParcels(jobId2);

    assertEquals(1, parcels1.size());
    assertEquals(1, parcels1.getFirst().getTiles().size());
    assertNotNull(parcels1.getFirst().getTiles().getFirst().getId());
    assertEquals(1, parcels2.size());
    assertEquals(1, parcels2.getFirst().getTiles().size());
    assertNotNull(parcels2.getFirst().getTiles().getFirst().getId());
  }

  @NotNull
  private ZoneTilingJob aZTJ(String jobId) {
    var job = new ZoneTilingJob();
    job.setId(jobId);
    job.setEmailReceiver("dummy@email.com");
    job.setZoneName("dummy");
    return job;
  }

  @SneakyThrows
  private static ParcelTilingTask aTask(
      String jobId, String taskId, String tileId, String parcelId) {
    var now = now();
    return ParcelTilingTask.builder()
        .id(taskId)
        .jobId(jobId)
        .submissionInstant(now)
        .parcels(
            List.of(
                Parcel.builder()
                    .id(parcelId)
                    .parcelContent(
                        ParcelContent.builder()
                            .geoServerUrl(
                                new URI("https://data.grandlyon.com/fr/geoserv/grandlyon/ows")
                                    .toURL())
                            .feature(
                                app.bpartners.geojobs.repository.model.Feature.builder().build())
                            .tiles(List.of(Tile.builder().id(tileId).creationDatetime(now).build()))
                            .creationDatetime(now)
                            .build())
                    .build()))
        .build();
  }
}
