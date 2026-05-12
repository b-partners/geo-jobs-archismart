package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStep;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.model.exception.ImageSourcesTimeoutException;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.DetectionDelimitationRetriever;
import app.bpartners.geojobs.service.SynchronousDetectionService;
import app.bpartners.geojobs.service.detection.DetectionTilingCreation;
import app.bpartners.geojobs.service.detection.MachineDetectionCreation;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.DetectionRoofPropertiesRequestedService;
import app.bpartners.geojobs.service.event.FeatureImageRequestedService;
import app.bpartners.geojobs.service.event.FeatureVggRequestedService;
import app.bpartners.geojobs.service.event.FeatureWithDetectionPropertiesRequestedService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SynchronousDetectionServiceTest {
  DetectionRepository detectionRepositoryMock = mock();
  DetectionFromStatisticRestMapper detectionFromStatisticRestMapperMock = mock();
  DetectionTilingCreation detectionTilingCreationMock = mock();
  ZoneTilingJobService zoneTilingJobServiceMock = mock();
  MachineDetectionCreation machineDetectionCreationMock = mock();
  DetectionDelimitationRetriever detectionDelimitationRetrieverMock = mock();
  FeatureVggRequestedService featureVggRequestedServiceMock = mock();
  GeoJsonConversionJobService geoJsonConversionJobServiceMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  Workers workers = new Workers();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  FeatureImageRequestedService featureImageRequestedServiceMock = mock();
  EntityManager entityManagerMock = mock();
  DetectionRoofPropertiesRequestedService detectionRoofPropertiesRequestedServiceMock = mock();
  FeatureWithDetectionPropertiesRequestedService
      featureWithDetectionPropertiesRequestedServiceMock = mock();
  SynchronousDetectionService subject =
      new SynchronousDetectionService(
          detectionRepositoryMock,
          detectionFromStatisticRestMapperMock,
          detectionTilingCreationMock,
          zoneTilingJobServiceMock,
          machineDetectionCreationMock,
          detectionDelimitationRetrieverMock,
          featureVggRequestedServiceMock,
          geoJsonConversionJobServiceMock,
          zoneDetectionJobServiceMock,
          workers,
          objectConfigurationRepositoryMock,
          featureImageRequestedServiceMock,
          entityManagerMock,
          detectionRoofPropertiesRequestedServiceMock,
          featureWithDetectionPropertiesRequestedServiceMock);

  @Test
  void return_succeeded_detection_and_trigger_geo_json_generation() {
    var detectionMock = mock(Detection.class);
    var detectionWithCreatedZTJMock = mock(Detection.class);
    var detectionWithCreatedZDJMock = mock(Detection.class);
    var parcelTilingTaskMock = mock(ParcelTilingTask.class);
    var tilingTasks = List.of(parcelTilingTaskMock);
    var finishedZoneTilingJobMock = mock(ZoneTilingJob.class);
    var createdZoneDetectionJob = mock(ZoneDetectionJob.class);
    var detectionWithVGGAndImagesFinished = mock(Detection.class);
    var restDetectionResult = mock(app.bpartners.geojobs.endpoint.rest.model.Detection.class);
    var zoneTilingJobId = randomUUID().toString();
    var zoneDetectionJobId = randomUUID().toString();
    var detectionId = randomUUID().toString();
    var feature = new Feature();

    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.needsImageOutput()).thenReturn(true);
    when(detectionMock.getDetectableObjectConfigurations())
        .thenReturn(List.of(new DetectableObjectConfiguration()));
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(feature));
    when(detectionWithVGGAndImagesFinished.getDelimitationOf(feature)).thenReturn(List.of(feature));
    when(detectionWithVGGAndImagesFinished.getVggFileKey())
        .thenReturn(null)
        .thenReturn("vggFileKey");
    when(detectionWithVGGAndImagesFinished.getProvidedGeoJsonZone()).thenReturn(List.of(feature));
    when(detectionWithCreatedZDJMock.getId()).thenReturn(detectionId);
    when(detectionWithVGGAndImagesFinished.getId()).thenReturn(detectionId);
    when(detectionWithCreatedZTJMock.getZtjId()).thenReturn(zoneTilingJobId);
    when(detectionWithCreatedZTJMock.toBuilder()).thenReturn(new Detection().toBuilder());
    when(createdZoneDetectionJob.getId()).thenReturn(zoneDetectionJobId);
    when(detectionTilingCreationMock.processTiling(detectionMock))
        .thenReturn(detectionWithCreatedZTJMock);
    when(zoneTilingJobServiceMock.consumeTasks(zoneTilingJobId)).thenReturn(tilingTasks);
    when(zoneTilingJobServiceMock.findById(zoneTilingJobId)).thenReturn(finishedZoneTilingJobMock);
    when(zoneDetectionJobServiceMock.saveZDJFromZTJ(finishedZoneTilingJobMock))
        .thenReturn(createdZoneDetectionJob);
    when(detectionRepositoryMock.save(any())).thenReturn(detectionWithCreatedZDJMock);
    doNothing().when(detectionDelimitationRetrieverMock).accept(detectionWithCreatedZTJMock);
    doNothing()
        .when(machineDetectionCreationMock)
        .processMachineDetection(detectionWithCreatedZDJMock, createdZoneDetectionJob, tilingTasks);
    doNothing()
        .when(featureVggRequestedServiceMock)
        .accept(new FeatureVggRequested(detectionId, null));
    when(detectionRepositoryMock.findById(detectionId))
        .thenReturn(Optional.of(detectionWithVGGAndImagesFinished));
    doNothing().when(entityManagerMock).clear();
    when(detectionRoofPropertiesRequestedServiceMock.apply(detectionId))
        .thenReturn(detectionWithVGGAndImagesFinished);
    when(featureWithDetectionPropertiesRequestedServiceMock.apply(
            detectionWithVGGAndImagesFinished, feature, List.of(feature)))
        .thenReturn(detectionWithVGGAndImagesFinished);

    when(restDetectionResult.getStep())
        .thenReturn(
            new DetectionStep()
                .name(MACHINE_DETECTION)
                .status(new Status().progression(FINISHED).health(SUCCEEDED)));

    when(detectionFromStatisticRestMapperMock.computeEmptyStatisticFromStep(
            eq(detectionWithVGGAndImagesFinished), any(), any(), any()))
        .thenReturn(restDetectionResult);

    var actual = subject.apply(detectionMock);

    verify(objectConfigurationRepositoryMock).saveAll(any());
    verify(geoJsonConversionJobServiceMock).getOrComputeGeoJsonConversionJob(any());
    assertEquals(MACHINE_DETECTION, actual.getStep().getName());
    assertEquals(FINISHED, actual.getStep().getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStep().getStatus().getHealth());
  }

  @Test
  void throws_image_sources_timeout_when_tiling_duration_exceeds_30_seconds_duration() {
    var detectionMock = mock(Detection.class);
    var detectionWithCreatedZTJMock = mock(Detection.class);
    var parcelTilingTaskMock = mock(ParcelTilingTask.class);
    doNothing().when(detectionDelimitationRetrieverMock).accept(detectionMock);
    when(detectionTilingCreationMock.processTiling(detectionMock))
        .thenReturn(detectionWithCreatedZTJMock);
    when(zoneTilingJobServiceMock.consumeTasks(any()))
        .thenAnswer(
            invocation -> {
              Thread.sleep(Duration.ofSeconds(31)); // TODO: deprecated but simulates real case
              return List.of(parcelTilingTaskMock);
            });

    ImageSourcesTimeoutException actual =
        assertThrows(ImageSourcesTimeoutException.class, () -> subject.apply(detectionMock));

    assertEquals(
        "Image sources are experiencing performance issues, which are preventing images from"
            + " loading.",
        actual.getMessage());
  }
}
