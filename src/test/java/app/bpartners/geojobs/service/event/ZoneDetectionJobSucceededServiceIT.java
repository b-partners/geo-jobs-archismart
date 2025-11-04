package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE_IMPORTANTE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ZoneDetectionJobSucceededServiceIT extends FacadeIT {
  private final String succeededJobId;
  private final String zoneTilingJobId;
  @MockBean ZoneDetectionJobService zoneDetectionJobService;
  @MockBean EventProducer eventProducer;
  @Autowired ZoneDetectionJobSucceededService subject;
  @MockBean DetectableObjectConfigurationRepository detectableObjectConfigurationRepositoryMock;
  @MockBean MachineDetectedTileRepository machineDetectedTileRepositoryMock;
  @Autowired private ZoneDetectionJobRepository jobRepository;

  ZoneDetectionJobSucceededServiceIT() {
    this.succeededJobId = randomUUID().toString();
    this.zoneTilingJobId = randomUUID().toString();
  }

  @BeforeEach
  void setUp() {
    jobRepository.saveAll(
        List.of(
            ZoneDetectionJob.builder()
                .id(succeededJobId)
                .detectionType(ZoneDetectionJob.DetectionType.MACHINE)
                .emailReceiver("emailReceiver")
                .zoneName("zoneName")
                .zoneTilingJob(
                    ZoneTilingJob.builder()
                        .id(zoneTilingJobId)
                        .zoneName("dummy")
                        .emailReceiver("dummy")
                        .build())
                .build()));
  }

  @AfterEach
  void tearDown() {
    jobRepository.deleteById(succeededJobId);
  }

  @Test
  void zdj_succeeds_trigger_delivery_job() {
    when(zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(any())).thenReturn(1L);
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            eq(succeededJobId), eq(USURE_IMPORTANTE.name())))
        .thenReturn(1L);
    when(detectableObjectConfigurationRepositoryMock.findAllByDetectionJobId(succeededJobId))
        .thenReturn(
            List.of(DetectableObjectConfiguration.builder().objectType(USURE_IMPORTANTE).build()));

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobSucceeded(succeededJobId)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventCaptor.capture());
    var capturedEvent = eventCaptor.getValue().getFirst();
    assertEquals(AnnotationDeliveryJobRequested.class, capturedEvent.getClass());
  }

  @Test
  void zdj_succeeds_trigger_conversion_job() {
    when(zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(any())).thenReturn(0L);
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            eq(succeededJobId), eq(USURE_IMPORTANTE.name())))
        .thenReturn(1L);
    when(detectableObjectConfigurationRepositoryMock.findAllByDetectionJobId(succeededJobId))
        .thenReturn(
            List.of(DetectableObjectConfiguration.builder().objectType(USURE_IMPORTANTE).build()));
    when(zoneDetectionJobService.findById(any())).thenReturn(new ZoneDetectionJob());

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobSucceeded(succeededJobId)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventCaptor.capture());
    var capturedEvent = eventCaptor.getValue().getFirst();
    assertEquals(GeoJsonConversionJobCreated.class, capturedEvent.getClass());
  }
}
