package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.model.FeatureImageRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.ImageSourcesTimeoutException;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.event.DetectionRoofPropertiesRequestedService;
import app.bpartners.geojobs.service.event.FeatureImageRequestedService;
import app.bpartners.geojobs.service.event.FeatureVggRequestedService;
import app.bpartners.geojobs.service.event.FeatureWithDetectionPropertiesRequestedService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SynchronousDetectionService
    implements Function<app.bpartners.geojobs.repository.model.detection.Detection, Detection> {
  private static final int MAX_RETRY_ATTEMPTS = 2;
  private final DetectionRepository detectionRepository;
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;
  private final DetectionTilingCreation detectionTilingCreation;
  private final ZoneTilingJobService zoneTilingJobService;
  private final MachineDetectionCreation machineDetectionCreation;
  private final DetectionDelimitationRetriever detectionDelimitationRetriever;
  private final FeatureVggRequestedService zoneVggRequestedService;
  private final GeoJsonConversionJobService geoJsonConversionJobService;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final Workers workers;
  private final DetectableObjectConfigurationRepository detectableObjectConfigurationRepository;
  private final FeatureImageRequestedService featureImageRequestedService;
  private final EntityManager entityManager;
  private final DetectionRoofPropertiesRequestedService detectionRoofPropertiesRequestedService;
  private final FeatureWithDetectionPropertiesRequestedService
      featureWithDetectionPropertiesRequestedService;

  @SneakyThrows
  @Override
  public Detection apply(app.bpartners.geojobs.repository.model.detection.Detection detection) {
    var delimitationRetrievingStart = now();
    detectionDelimitationRetriever.accept(detection);
    log.info(
        "Retrieving delimitation finished in {} seconds for detection(e2Id={}, feature={})",
        Duration.between(delimitationRetrievingStart, now()).toSeconds(),
        detection.getEndToEndId(),
        detection.getFeatureWithDelimitations());

    // Tiling step
    var tilingJobStart = now();
    var detectionWithCreatedZTJ = detectionTilingCreation.processTiling(detection);
    var zoneTilingJobId = detectionWithCreatedZTJ.getZtjId();
    var tilingTasks = zoneTilingJobService.consumeTasks(zoneTilingJobId);
    var finishedZoneTilingJob = zoneTilingJobService.findById(zoneTilingJobId);
    var tilingJobProcessingDuration = Duration.between(tilingJobStart, now()).toSeconds();
    log.info(
        "Tiling job finished in {} seconds for detection(e2Id={}, tilingTasks.size={})",
        tilingJobProcessingDuration,
        detection.getEndToEndId(),
        tilingTasks.size());
    if (tilingJobProcessingDuration > 30L) {
      throw new ImageSourcesTimeoutException(
          String.format(
              "Image sources%s are experiencing performance issues, which are preventing images"
                  + " from loading.",
              detection.getGeoServerProperties() == null
                      || detection.getGeoServerProperties().getGeoServerParameter() == null
                      || detection.getGeoServerProperties().getGeoServerParameter().getLayers()
                          == null
                  ? ""
                  : " from "
                      + detection.getGeoServerProperties().getGeoServerParameter().getLayers()));
    }

    // Machine detection job creation
    var parallelMachineDetectionAndImageRequestStart = now();
    var createdZoneDetectionJob = zoneDetectionJobService.saveZDJFromZTJ(finishedZoneTilingJob);
    var zoneDetectionJobDetectableConf =
        detection.getDetectableObjectConfigurations().stream()
            .map(
                detectableObjectConfiguration ->
                    detectableObjectConfiguration.duplicate(
                        randomUUID().toString(), createdZoneDetectionJob.getId()))
            .toList();
    detectableObjectConfigurationRepository.saveAll(zoneDetectionJobDetectableConf);

    var detectionWithCreatedZDJ =
        detectionRepository.save(
            detectionWithCreatedZTJ.toBuilder().zdjId(createdZoneDetectionJob.getId()).build());

    var feature = detection.getProvidedGeoJsonZone().getFirst();
    Callable<Void> imageRequestCallableVoidList =
        () -> {
          featureImageRequestedService.accept(
              new FeatureImageRequested(detection.getId(), feature));
          return null;
        };
    Callable<Void> machineDetectionProcessCallableVoidList =
        () -> {
          // Machine detection step
          machineDetectionCreation.processMachineDetection(
              detectionWithCreatedZDJ, createdZoneDetectionJob, tilingTasks);
          return null;
        };
    List<Callable<Void>> firstCallableVoidList = new ArrayList<>();
    if (detection.needsImageOutput()) {
      firstCallableVoidList.add(imageRequestCallableVoidList);
    }
    firstCallableVoidList.add(machineDetectionProcessCallableVoidList);
    workers.invokeAll(firstCallableVoidList);
    log.info(
        "Machine detection and image request finished in {} seconds for detection(e2Id={},"
            + " imagesCount={})",
        Duration.between(parallelMachineDetectionAndImageRequestStart, now()).toSeconds(),
        detection.getEndToEndId(),
        tilingTasks.size());

    var rooPropertiesComputingStart = now();
    var detectionWithComputedRoofProperties =
        detectionRoofPropertiesRequestedService.apply(detectionWithCreatedZDJ.getId());
    log.info(
        "Detection roof properties computing finished in {} seconds for detection(e2Id={})",
        Duration.between(rooPropertiesComputingStart, now()).toSeconds(),
        detection.getEndToEndId());

    var detectionResultPropertiesComputingStart = now();
    var detectionWithResultProperties =
        featureWithDetectionPropertiesRequestedService.apply(
            detectionWithComputedRoofProperties,
            detectionWithComputedRoofProperties.getProvidedGeoJsonZone().getFirst(),
            detectionWithComputedRoofProperties.getDelimitationOf(
                detectionWithComputedRoofProperties.getProvidedGeoJsonZone().getFirst()));
    log.info(
        "Detection result properties computing finished in {} seconds for detection(e2Id={})",
        Duration.between(detectionResultPropertiesComputingStart, now()).toSeconds(),
        detection.getEndToEndId());

    var vggRequestAndGeoJsonEventTriggerStart = now();
    Callable<Void> featureVggRequestedCallableVoid =
        () -> {
          // VGG result computing step
          zoneVggRequestedService.accept(
              new FeatureVggRequested(
                  detectionWithResultProperties.getId(),
                  detectionWithResultProperties.getProvidedGeoJsonZone().getFirst()));
          return null;
        };
    Callable<Void> geoJsonRequestedCallableVoid =
        () -> {
          // GeoJson Result Requested __EVENT__ step
          geoJsonConversionJobService.getOrComputeGeoJsonConversionJob(createdZoneDetectionJob);
          return null;
        };

    List<Callable<Void>> secondVoidCallable = new ArrayList<>();
    if (detection.needsImageOutput()) {
      secondVoidCallable.add(featureVggRequestedCallableVoid);
    }
    secondVoidCallable.add(geoJsonRequestedCallableVoid);
    workers.invokeAll(secondVoidCallable);
    log.info(
        "GeoJSON conversion request and VGG computation done in {} seconds for detection(e2Id={})",
        Duration.between(vggRequestAndGeoJsonEventTriggerStart, now()).toSeconds(),
        detection.getEndToEndId());

    return detectionWithResultProperties.hasToitureModelName()
        ? attemptVggFileKeyRetrieve(detectionWithResultProperties)
        : detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
            detectionRepository.findById(detection.getId()).orElseThrow(),
            FINISHED,
            SUCCEEDED,
            MACHINE_DETECTION);
  }

  @SneakyThrows
  private Detection attemptVggFileKeyRetrieve(
      app.bpartners.geojobs.repository.model.detection.Detection detection) {
    var retrievingVggFileKeyStart = now();
    for (int attempt = 0; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
      entityManager.clear();
      var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();

      if (actualDetection.getVggFileKey() != null) {
        return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
            actualDetection, FINISHED, SUCCEEDED, MACHINE_DETECTION);
      }

      if (attempt < MAX_RETRY_ATTEMPTS) {
        log.info(
            "VGG fileKey still null for detection.e2Id: {} (attempt {}/{}) → waiting 5s before"
                + " retry",
            detection.getEndToEndId(),
            attempt + 1,
            MAX_RETRY_ATTEMPTS);
        Thread.sleep(Duration.ofSeconds(5L));
      }
    }

    log.info(
        "VGG file key retrieved in {} seconds for detection(e2Id={})",
        Duration.between(retrievingVggFileKeyStart, now()).toSeconds(),
        detection.getEndToEndId());

    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        detectionRepository.findById(detection.getId()).orElseThrow(),
        FINISHED,
        SUCCEEDED,
        MACHINE_DETECTION);
  }
}
