package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.event.ParcelDetectionJobCreatedService;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Slf4j
public class ParcelDetectionJobCreatedServiceTest {
  private static final String JOB_ID = "jobId";
  TileDetectionTaskRepository taskRepositoryMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectableObjectConfigurationMapper objectConfigurationMapperMock = mock();
  ParcelDetectionJobCreatedService subject =
      new ParcelDetectionJobCreatedService(
          taskRepositoryMock,
          objectConfigurationRepositoryMock,
          eventProducerMock,
          detectionRepositoryMock);

  @Test
  void accept_from_persisted_object_conf_ok() {
    var parcelDetectionJob = ParcelDetectionJob.builder().id(JOB_ID).build();
    var zoneDetectionJobId = "zoneDetectionJobId";
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(List.of(aTileDetectionTask("task1"), aTileDetectionTask("task2")));
    List<DetectableObjectConfiguration> objectConfigurations =
        List.of(
            DetectableObjectConfiguration.builder()
                .id("objectConfiguration1")
                .objectType(PASSAGE_PIETON)
                .build());
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(zoneDetectionJobId))
        .thenReturn(objectConfigurations);

    assertDoesNotThrow(
        () ->
            subject.accept(new ParcelDetectionJobCreated(zoneDetectionJobId, parcelDetectionJob)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskRepositoryMock, times(1)).findAllByJobId(JOB_ID);
    verify(objectConfigurationRepositoryMock, times(1)).findAllByDetectionJobId(zoneDetectionJobId);
    verify(eventProducerMock, times(3)).accept(eventCaptor.capture());
    var events = eventCaptor.getAllValues();
    var event1 = (List<TileDetectionTaskCreated>) events.get(1);
    var event2 = (List<TileDetectionTaskCreated>) events.getLast();
    assertTrue(
        event1.contains(
            new TileDetectionTaskCreated(
                zoneDetectionJobId,
                aTileDetectionTask("task1"),
                objectConfigurations,
                null,
                null)));
    assertTrue(
        event2.contains(
            new TileDetectionTaskCreated(
                zoneDetectionJobId,
                aTileDetectionTask("task2"),
                objectConfigurations,
                null,
                null)));
  }

  @Test
  void accept_from_detection_ok_ok() {
    var parcelDetectionJob = ParcelDetectionJob.builder().id(JOB_ID).build();
    var zoneDetectionJobId = "zoneDetectionJobId";
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(List.of(aTileDetectionTask("task1"), aTileDetectionTask("task2")));
    List<DetectableObjectConfiguration> objectConfigurations =
        List.of(
            DetectableObjectConfiguration.builder()
                .id("objectConfiguration1")
                .objectType(PASSAGE_PIETON)
                .build());
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(zoneDetectionJobId))
        .thenReturn(List.of());
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(
            Optional.of(
                Detection.builder().detectableObjectConfigurations(objectConfigurations).build()));

    assertDoesNotThrow(
        () ->
            subject.accept(new ParcelDetectionJobCreated(zoneDetectionJobId, parcelDetectionJob)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskRepositoryMock, times(1)).findAllByJobId(JOB_ID);
    verify(objectConfigurationRepositoryMock, times(1)).findAllByDetectionJobId(zoneDetectionJobId);
    verify(eventProducerMock, times(3)).accept(eventCaptor.capture());
    var events = eventCaptor.getAllValues();
    var event1 = (List<TileDetectionTaskCreated>) events.get(1);
    var event2 = (List<TileDetectionTaskCreated>) events.getLast();
    assertTrue(
        event1.contains(
            new TileDetectionTaskCreated(
                zoneDetectionJobId,
                aTileDetectionTask("task1"),
                objectConfigurations,
                null,
                null)));
    assertTrue(
        event2.contains(
            new TileDetectionTaskCreated(
                zoneDetectionJobId,
                aTileDetectionTask("task2"),
                objectConfigurations,
                null,
                null)));
  }

  private TileDetectionTask aTileDetectionTask(String id) {
    return TileDetectionTask.builder().id(id).build();
  }
}
