package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DelimitationType.PARCEL_FREE_DELIMITATION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;
import static app.bpartners.geojobs.repository.model.detection.DetectionFeatureType.PROVIDED_FEATURE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.service.detection.DetectionCreationMapper.getOrSetFeatureIdentifier;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.ZIP_BUCKET_FOLDER;
import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.*;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.zone.DetectionQualityControlFinished;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DetectionStepMapper;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.detection.DetectionCreationMapper;
import app.bpartners.geojobs.service.event.FeatureVggRequestedService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
@Slf4j
public class DetectionService {
  private static final int DEFAULT_ZOOM = 20;
  private static final Instant BEGINNING_OF_2026 = parse("2026-01-01T00:00:00Z");
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneTilingJobService zoneTilingJobService;
  private final EventProducer eventProducer;
  private final DetectionRepository detectionRepository;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final BucketComponent bucketComponent;
  private final GeoJsonConversionJobService conversionInitiationService;
  private final ObjectMapper objectMapper;
  private final AuthProvider authProvider;
  private final DetectionGeoJsonUpdateValidator detectionGeoJsonUpdateValidator;
  private final CommunityAuthorizationRepository communityAuthRepository;
  private final DetectionTilingCreation detectionTilingCreation;
  private final MachineDetectionCreation machineDetectionCreation;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;
  private final DetectionAddressConsumer detectionAddressConsumer;
  private final SynchronousDetectionService synchronousDetectionService;
  private final SynchronousDetectionValidator synchronousDetectionValidator;
  private final DetectionStepMapper detectionStepMapper;
  private final RoofAnalysisMailer roofAnalysisMailer;
  private final DetectionCreationMapper detectionCreationMapper;
  private final FileWriter fileWriter;
  private final DetectionRoofSlopeValidator detectionRoofSlopeValidator;
  private final FeatureVggRequestedService featureVggRequestedService;

  public Detection getByZoneDetectionJob(ZoneDetectionJob zoneDetectionJob) {
    ZoneDetectionJob machineZDJ =
        zoneDetectionJobService.getMachineZdjFromZdjId(zoneDetectionJob.getId());
    ZoneDetectionJob humanZDJ;
    try {
      humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJob.getId());
    } catch (IllegalArgumentException ignored) {
      humanZDJ = null;
    }
    return detectionRepository
        .findByZdjId(humanZDJ == null ? null : humanZDJ.getId())
        .orElseGet(
            () -> {
              var optionalDetectionFromMachineZDJ =
                  detectionRepository.findByZdjId(machineZDJ.getId());
              if (optionalDetectionFromMachineZDJ.isPresent()) {
                return optionalDetectionFromMachineZDJ.orElseThrow();
              }
              return null;
            });
  }

  public Detection computeRoofsProperties(String detectionE2Id) {
    var detection =
        detectionRepository
            .findByEndToEndId(detectionE2Id)
            .orElseThrow(
                () -> new NotFoundException("Detection.e2Id " + detectionE2Id + " not found."));

    detectionRoofSlopeValidator.accept(detection);

    if (detection.getRoofPropertiesComputationCreationDatetime() == null) {
      eventProducer.accept(
          List.of(
              DetectionRoofSlopeAndHeightRequested.builder()
                  .detectionId(detection.getId())
                  .build()));
    }

    return getProcessedDetection(detection.getEndToEndId());
  }

  public List<DetectionFileObject> getDetectionFileObjects(String detectionE2Id) {
    var detection =
        detectionRepository
            .findByEndToEndId(detectionE2Id)
            .orElseThrow(
                () -> new NotFoundException("Detection.id= " + detectionE2Id + " not found."));

    return detection.getFileObjects();
  }

  private List<Feature> readFromFile(File featuresFromShape) {
    try {
      var featuresFileContent = Files.readString(featuresFromShape.toPath());
      List<Feature> features =
          objectMapper.readValue(featuresFileContent, new TypeReference<>() {});
      return features.stream()
          .peek(feature -> feature.getProperties().put("zoom", DEFAULT_ZOOM))
          .toList();
    } catch (Exception e) {
      throw new ApiException(
          CLIENT_EXCEPTION, "Unable to convert uploaded file to Features, exception=" + e);
    }
  }

  public Detection finalizeGeoJsonConfig(String detectionId, File featuresFromShape) {
    var detection = getDetectionById(detectionId);
    if (detection.getProvidedGeoJsonZone() != null
        && !detection.getProvidedGeoJsonZone().isEmpty()) {
      throw new BadRequestException(
          "Unable to finalize Detection(id=" + detectionId + ") geoJson as it already has values");
    }
    var features =
        readFromFile(featuresFromShape).stream().map(FeatureMapper::toDomainFeature).toList();
    detection.setProvidedGeoJsonZone(features);
    detection.addFeatures(features, PROVIDED_FEATURE);

    var savedDetection = detectionRepository.save(detection);

    eventProducer.accept(
        List.of(DetectionSaved.builder().detectionIdentifier(savedDetection.getId()).build()));
    return withComputedStep(savedDetection, PROCESSING, UNKNOWN, REQUEST_ACCEPTED);
  }

  private Detection getDetectionById(String detectionId) {
    return detectionRepository
        .findById(detectionId)
        .orElseThrow(() -> new NotFoundException("Detection(id=" + detectionId + ") not found"));
  }

  private Detection getDetectionByE2eId(String detectionId, String communityOwnerId) {
    return detectionRepository
        .findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Detection(e2e.id="
                        + detectionId
                        + ") not found for communityOwnerId="
                        + communityOwnerId));
  }

  public Detection configureExcelFile(String detectionId, File excelFile) {
    var detection = getDetectionByE2IdOrId(detectionId);
    detectionGeoJsonUpdateValidator.accept(detection);
    var bucketKey = "detections/excel/" + detectionId + ".xlsx";
    bucketComponent.upload(excelFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().excelFileKey(bucketKey).build());
    eventProducer.accept(
        List.of(DetectionExcelFileSaved.builder().detection(savedDetection).build()));
    eventProducer.accept(
        List.of(DetectionSaved.builder().detectionIdentifier(savedDetection.getId()).build()));
    return withComputedStep(savedDetection, PENDING, UNKNOWN, REQUEST_ACCEPTED);
  }

  public Detection configureDetectionAddresses(String detectionId, List<String> addresses) {
    var detection = getDetectionByE2IdOrId(detectionId);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().convertedAddresses(addresses).build());

    detectionAddressConsumer.accept(savedDetection);

    return withComputedStep(savedDetection, PROCESSING, UNKNOWN, REQUEST_ACCEPTED);
  }

  public Detection configureShapeFile(String detectionId, File shapeFile) {
    var detection = getDetectionByE2IdOrId(detectionId);
    detectionGeoJsonUpdateValidator.accept(detection);
    var bucketKey = "detections/shape/" + detectionId;
    bucketComponent.upload(shapeFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().shapeFileKey(bucketKey).build());
    eventProducer.accept(
        List.of(DetectionSaved.builder().detectionIdentifier(savedDetection.getId()).build()));
    return withComputedStep(savedDetection, PENDING, UNKNOWN, REQUEST_ACCEPTED);
  }

  public Detection uploadPdfFile(String detectionId, File imageFile) {
    var detection = getDetectionByE2IdOrId(detectionId);
    var bucketKey = "detections/roofer/pdf/" + detectionId + ".pdf";
    bucketComponent.upload(imageFile, bucketKey);
    var savedDetection =
        detectionRepository.save(detection.toBuilder().pdfFileKey(bucketKey).build());
    eventProducer.accept(
        List.of(DetectionSaved.builder().detectionIdentifier(savedDetection.getId()).build()));
    return withComputedStep(savedDetection, FINISHED, SUCCEEDED, MACHINE_DETECTION);
  }

  public Detection configureFileResult(
      String communityId, String detectionE2eId, MultipartFile file, String extensionType)
      throws IOException {
    if (communityId == null) {
      throw new IllegalArgumentException("To sumbit result, communityAuthorizationId is mandatory");
    }
    var detection = getDetectionByE2eId(detectionE2eId, communityId);
    String extension = "." + extensionType.toLowerCase();
    var resultFileKey =
        GEO_JSON_EXTENSION.contains(extension)
            ? GEO_JSON_BUCKET_FOLDER + detection.getId() + "/" + detection.getZoneName() + extension
            : ZIP_BUCKET_FOLDER + detection.getId() + "/" + detection.getZoneName() + extension;
    byte[] fileBytes = file.getBytes();
    File toUpload = fileWriter.apply(fileBytes, null);
    bucketComponent.upload(toUpload, resultFileKey);

    var savedDetection =
        detectionRepository.save(detection.toBuilder().geojsonS3FileKey(resultFileKey).build());

    eventProducer.accept(
        List.of(DetectionSaved.builder().detectionIdentifier(savedDetection.getId()).build()));
    eventProducer.accept(
        List.of(DetectionQualityControlFinished.builder().detection(savedDetection).build()));

    if (!savedDetection.isOnStepPostProcessingSucceeded()) {
      return updateDetectionStep(
          savedDetection.getEndToEndId(),
          communityId,
          new DetectionStep()
              .name(POST_PROCESSING)
              .status(
                  new app.bpartners.geojobs.endpoint.rest.model.Status()
                      .progression(
                          app.bpartners.geojobs.endpoint.rest.model.Status.ProgressionEnum.FINISHED)
                      .health(app.bpartners.geojobs.endpoint.rest.model.Status.HealthEnum.SUCCEEDED)
                      .creationDatetime(now()))
              .statistics(List.of())
              .updatedAt(now()));
    }

    return withComputedStep(detection, FINISHED, SUCCEEDED, POST_PROCESSING);
  }

  public Detection getProcessedDetection(String detectionId) {
    var detection = getDetectionByE2IdOrId(detectionId);
    if (detection.getStep() != null) {
      // the persisted step takes priority and is exposed through Detection#getCurrentStep
      return detection;
    }
    if (detection.isSucceeded()) {
      return withComputedStep(detection, FINISHED, SUCCEEDED, POST_PROCESSING);
    }
    if (detection.isStillOnConfiguringStep()) {
      return withComputedStep(detection, PENDING, UNKNOWN, REQUEST_ACCEPTED);
    }
    if (detection.isStillOnTilingStep()) {
      if (detection.isTilingPending()) {
        return withComputedStep(detection, PENDING, UNKNOWN, TILING);
      }
      return withTilingStatistic(detection, detection.getZtjId());
    }
    var zoneDetectionJob = zoneDetectionJobService.findById(detection.getZdjId());
    if (detection.isMachineDetectionStepProcessing(zoneDetectionJob)) {
      return withMachineDetectionStatistic(detection, detection.getZdjId());
    }
    if (detection.isPostProcessingStep(zoneDetectionJob)) {
      var inDoubtDetectedTileToDelivery =
          zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(zoneDetectionJob.getId());
      if (inDoubtDetectedTileToDelivery > 0 && detection.isAnnotationDeliveryEnable()) {
        return withComputedStep(detection, PROCESSING, UNKNOWN, POST_PROCESSING);
      }
      var geoJsonConversionJob = findActualGeoJsonConversionJob(zoneDetectionJob.getId());
      if (geoJsonConversionJob == null && zoneDetectionJob.isSucceeded()) {
        return withComputedStep(detection, FINISHED, FAILED, POST_PROCESSING);
      }
      if (geoJsonConversionJob != null) {
        if (geoJsonConversionJob.isFailed()) {
          return withComputedStep(detection, FINISHED, FAILED, POST_PROCESSING);
        }
        if (geoJsonConversionJob.isProcessing()
            || (geoJsonConversionJob.isSucceeded() && detection.getGeojsonS3FileKey() == null)) {
          return withComputedStep(detection, PROCESSING, UNKNOWN, POST_PROCESSING);
        }
        if (geoJsonConversionJob.isPending()) {
          return withComputedStep(detection, PENDING, UNKNOWN, POST_PROCESSING);
        }
      }
      return withMachineDetectionStatistic(detection, detection.getZdjId());
    }
    throw new IllegalStateException(
        "Detection(id=" + detection.getId() + ") processing failed on illegal state");
  }

  private Detection getDetectionByE2IdOrId(String detectionId) {
    var communityAuthorization =
        communityAuthRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    var communityOwnerId = communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
    return communityOwnerId != null
        ? getDetectionByE2eId(detectionId, communityOwnerId)
        : getDetectionById(detectionId);
  }

  public Detection processDetectionSynchronously(
      String detectionId,
      CreateDetection createDetection,
      String communityOwnerId,
      Boolean debugMode) {
    var validatedCreateDetection = synchronousDetectionValidator.apply(createDetection);

    var optionalDetection =
        detectionRepository.findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId);
    Detection detectionToBeProcessed;
    detectionToBeProcessed =
        optionalDetection.orElseGet(
            () ->
                createDetectionJob(
                    detectionId, validatedCreateDetection, communityOwnerId, true, debugMode));
    var features =
        validatedCreateDetection.getGeoJsonZone().stream()
            .peek(getOrSetFeatureIdentifier(Feature::getProperties, Feature::setProperties))
            .map(FeatureMapper::toDomainFeature)
            .toList();
    var detectionToSave = detectionToBeProcessed.toBuilder().providedGeoJsonZone(features).build();
    detectionToSave.addFeatures(features, PROVIDED_FEATURE);
    var savedDetectionToBeProcessed = detectionRepository.save(detectionToSave);

    try {
      synchronousDetectionService.apply(savedDetectionToBeProcessed);
    } catch (RuntimeException e) {
      markSynchronousDetectionFailed(savedDetectionToBeProcessed.getId(), e);
      throw e;
    }
    var processedDetection =
        detectionRepository.findById(savedDetectionToBeProcessed.getId()).orElseThrow();
    return withComputedStep(processedDetection, FINISHED, SUCCEEDED, MACHINE_DETECTION);
  }

  private void markSynchronousDetectionFailed(String detectionId, RuntimeException e) {
    try {
      var detection = detectionRepository.findById(detectionId).orElse(null);
      if (detection == null) {
        return;
      }
      var failedStepName = failedStepNameOf(detection);
      detection.addStep(
          app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
              .id(randomUUID().toString())
              .detectionId(detection.getId())
              .name(failedStepName)
              .progression(FINISHED)
              .health(FAILED)
              .message(e.getMessage())
              .creationDatetime(now())
              .build());
      detectionRepository.save(detection);
      log.error(
          "Synchronous detection(id={}) marked FAILED on step {}", detectionId, failedStepName, e);
    } catch (RuntimeException persistenceException) {
      // never mask the original failure because the FAILED step could not be persisted
      log.error(
          "Could not persist FAILED step for synchronous detection(id={})",
          detectionId,
          persistenceException);
    }
  }

  /** Furthest pipeline step reached, inferred from the artifacts already persisted. */
  private DetectionStepName failedStepNameOf(Detection detection) {
    if (detection.getZdjId() != null) {
      return MACHINE_DETECTION;
    }
    if (detection.getZtjId() != null) {
      return TILING;
    }
    return REQUEST_ACCEPTED;
  }

  public Detection processDetection(
      String detectionId,
      CreateDetection createDetection,
      String communityOwnerId,
      Boolean debugMode) {
    if (createDetection.getGeoJsonZone() == null) {
      createDetection.setGeoJsonZone(new ArrayList<>());
    }
    if (createDetection.getGeoJsonZone() != null) {
      createDetection
          .getGeoJsonZone()
          .forEach(
              feature -> {
                var properties = feature.getProperties();
                if (properties != null) {
                  properties.remove("id");
                }
              });
    }
    if (createDetection.getZoneName() != null && createDetection.getZoneName().contains(".")) {
      createDetection.setZoneName(createDetection.getZoneName().replaceAll("\\.", "_"));
    }
    var optionalDetection =
        detectionRepository.findByEndToEndIdAndCommunityOwnerId(detectionId, communityOwnerId);

    if (optionalDetection.isEmpty()) {
      var savedDetection =
          createDetectionJob(detectionId, createDetection, communityOwnerId, false, debugMode);
      if (savedDetection.isStillOnConfiguringStep()) {
        return withComputedStep(savedDetection, PENDING, UNKNOWN, REQUEST_ACCEPTED);
      }
      eventProducer.accept(List.of(new DetectionTilingRequested(savedDetection.getId())));
      return withComputedStep(savedDetection, PROCESSING, UNKNOWN, REQUEST_ACCEPTED);
    }
    return processDetectionSteps(optionalDetection.get());
  }

  public Detection processDetectionSteps(Detection detection) {
    var tilingJobId = detection.getZtjId();
    var detectionJobId = detection.getZdjId();
    if (detection.isStillOnConfiguringStep()) {
      return withComputedStep(detection, PENDING, UNKNOWN, REQUEST_ACCEPTED);
    }
    if (tilingJobId == null) {
      var detectionWithZtj = detectionTilingCreation.processTiling(detection);
      return withTilingStatistic(detectionWithZtj, detectionWithZtj.getZtjId());
    }
    var zoneTilingJob = zoneTilingJobService.findById(tilingJobId);
    if (!zoneTilingJob.isSucceeded()) {
      return withTilingStatistic(detection, tilingJobId);
    }
    var machineZoneDetectionJob = zoneDetectionJobService.findById(detectionJobId);

    if (machineZoneDetectionJob.isPending() && zoneTilingJob.isFinished()) {
      machineDetectionCreation.apply(detection, zoneTilingJob);
    }
    if (machineZoneDetectionJob.isFinished()) {
      if (zoneDetectionJobService.countInDoubtDetectedTileToDeliveryById(detectionJobId) == 0L
          && !detection.isAnnotationDeliveryEnable()) {
        processVerificationOrGenerateGeoJson(detection, machineZoneDetectionJob);
      } else {
        var humanZoneDetectionJob = zoneDetectionJobService.getByTilingJobId(tilingJobId, HUMAN);
        processVerificationOrGenerateGeoJson(detection, humanZoneDetectionJob);
      }
    }
    return withMachineDetectionStatistic(detection, machineZoneDetectionJob.getId());
  }

  private void processVerificationOrGenerateGeoJson(
      Detection detection, ZoneDetectionJob zoneDetectionJob) {
    if (HUMAN.equals(zoneDetectionJob.getDetectionType()) && !zoneDetectionJob.isSucceeded()) {
      eventProducer.accept(
          List.of(
              AnnotationJobVerificationSent.builder()
                  .humanZdjId(zoneDetectionJob.getId())
                  .build()));
    } else {
      conversionInitiationService.getOrComputeGeoJsonConversionJob(detection, zoneDetectionJob);
    }
  }

  private Detection createDetectionJob(
      String detectionE2Id,
      CreateDetection createDetection,
      @Nullable String communityOwnerId,
      boolean isSynchronous,
      Boolean debugMode) {
    if (createDetection.getGeoJsonDelimitationType() == null) {
      log.info(
          "Setting default geoJsonDelimitationType to PARCEL_FREE_DELIMITATION for detection.e2Id"
              + " {}",
          detectionE2Id);
      createDetection.setGeoJsonDelimitationType(PARCEL_FREE_DELIMITATION);
    }
    var detectionToSave =
        detectionCreationMapper.apply(
            createDetection, detectionE2Id, communityOwnerId, isSynchronous, debugMode);
    List<Feature> geoJsonZone =
        createDetection.getGeoJsonZone() == null ? List.of() : createDetection.getGeoJsonZone();
    var detectionToSaveBuilder = detectionToSave.toBuilder();
    if (communityOwnerId != null) {
      var communityOwner = communityAuthRepository.findById(communityOwnerId).orElseThrow();
      if (communityOwner.isIntegrationTestUsage()) {
        detectionToSaveBuilder.integrationTest(true);
      }
    }
    var savedDetection =
        communityUsedSurfaceService.persistDetectionWithSurfaceUsage(
            detectionToSaveBuilder.build(), geoJsonZone);

    if (!savedDetection.isIntegrationTest()) {
      eventProducer.accept(
          List.of(DetectionSaved.builder().detectionIdentifier(savedDetection.getId()).build()));
    }
    return savedDetection;
  }

  public List<Detection> getDetectionsByCriteria(
      Optional<String> communityId,
      PageFromOne page,
      BoundedPageSize pageSize,
      Instant fromParameter,
      Instant toParameter,
      Optional<String> optionalZoneName) {
    final Instant from = fromParameter == null ? BEGINNING_OF_2026 : fromParameter;
    final Instant to = toParameter == null ? now() : toParameter;
    var pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    var detections =
        communityId
            .map(
                ownerId ->
                    optionalZoneName
                        .map(
                            zoneName ->
                                detectionRepository
                                    .findByCommunityOwnerIdAndCreationDatetimeBetweenAndZoneNameIsContainingIgnoreCaseOrderByCreationDatetimeDesc(
                                        ownerId, from, to, zoneName, pageable))
                        .orElseGet(
                            () ->
                                detectionRepository
                                    .findByCommunityOwnerIdAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
                                        ownerId, from, to, pageable)))
            .orElseGet(
                () ->
                    optionalZoneName
                        .map(
                            zoneName ->
                                detectionRepository
                                    .findAllByCreationDatetimeBetweenAndZoneNameIsContainingIgnoreCaseOrderByCreationDatetimeDesc(
                                        from, to, zoneName, pageable))
                        .orElseGet(
                            () ->
                                detectionRepository
                                    .findAllByCreationDatetimeBetweenOrderByCreationDatetimeDesc(
                                        from, to, pageable)));

    return detections.stream()
        .map(detection -> detection.toBuilder().id(detection.getEndToEndId()).build())
        .map(this::withStepStatistics)
        .toList();
  }

  private Detection withStepStatistics(Detection detection) {
    if (detection.getStep() != null) {
      // the persisted step takes priority and is exposed through Detection#getCurrentStep
      return detection;
    }
    if (detection.getZdjId() != null) {
      return withStatistic(
          detection,
          MACHINE_DETECTION,
          zoneDetectionJobService.getTaskStatistic(detection.getZdjId()));
    }
    if (detection.getZtjId() != null) {
      return withStatistic(
          detection, TILING, zoneTilingJobService.getTaskStatistic(detection.getZtjId()));
    }
    return withStatistic(detection, REQUEST_ACCEPTED, new TaskStatistic());
  }

  private GeoJsonConversionJob findActualGeoJsonConversionJob(String zoneDetectionJobId) {
    var geoJsonConversionJobs =
        geoJsonConversionJobRepository.findByZoneDetectionJobId(zoneDetectionJobId);
    return geoJsonConversionJobs.stream()
        .max(Comparator.comparing(Job::getSubmissionInstant))
        .orElse(null);
  }

  public Detection sendMailAboutProspect(String detectionId, Prospect prospect) {
    var detection = detectionRepository.findByEndToEndId(detectionId).orElseThrow();
    var pdfFile = bucketComponent.download(detection.getPdfFileKey());
    roofAnalysisMailer.accept(prospect, pdfFile);
    return withComputedStep(detection, FINISHED, SUCCEEDED, MACHINE_DETECTION);
  }

  public Detection updateDetectionStep(
      String detectionId, String communityOwnerId, DetectionStep step) {
    Detection detection =
        communityOwnerId == null
            ? getDetectionByE2IdOrId(detectionId)
            : getDetectionByE2eId(detectionId, communityOwnerId);

    var domainStep = detectionStepMapper.toDomain(detection.getId(), step);
    detection.addStep(domainStep);
    detectionRepository.save(detection);

    if (detection.isToNotify()) {
      eventProducer.accept(List.of(DetectionStepUpdated.builder().detection(detection).build()));
    }

    // the freshly added persisted step takes priority, exposed through Detection#getCurrentStep
    return detection;
  }

  private static Detection withComputedStep(
      Detection detection,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      DetectionStepName name) {
    detection.setComputedStep(
        app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
            .name(name)
            .progression(progression)
            .health(health)
            .creationDatetime(now())
            .build());
    return detection;
  }

  private Detection withTilingStatistic(Detection detection, String tilingJobId) {
    return withStatistic(
        detection, TILING, zoneTilingJobService.computeTaskStatistics(tilingJobId));
  }

  private Detection withMachineDetectionStatistic(Detection detection, String zdjId) {
    return withStatistic(
        detection, MACHINE_DETECTION, zoneDetectionJobService.computeTaskStatistics(zdjId));
  }

  private static Detection withStatistic(
      Detection detection, DetectionStepName name, TaskStatistic statistic) {
    var actualJobStatus = statistic.getActualJobStatus();
    detection.setComputedStep(
        app.bpartners.geojobs.repository.model.detection.DetectionStep.builder()
            .name(name)
            .progression(actualJobStatus == null ? null : actualJobStatus.getProgression())
            .health(actualJobStatus == null ? null : actualJobStatus.getHealth())
            .creationDatetime(now())
            .statistic(statistic)
            .build());
    return detection;
  }

  public Detection producesVggComputing(String detectionIdentifier) {
    var detection =
        detectionRepository
            .findById(detectionIdentifier)
            .orElseThrow(
                () -> new NotFoundException("Detection.id=" + detectionIdentifier + " not found"));
    if (detection.getVggFileKey() != null) {
      throw new BadRequestException(
          "Detection.id=" + detectionIdentifier + " already has computed its VGG");
    }
    if (!detection.needsImageOutput()) {
      throw new BadRequestException(
          "Detection.id="
              + detectionIdentifier
              + " does not need image output so can not produce VGG");
    }
    var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
    if (providedGeoJsonZone.size() == 1) {
      var detectionWithVgg =
          featureVggRequestedService.apply(detection, providedGeoJsonZone.getFirst());
      return withStepStatistics(detectionWithVgg);
    }
    providedGeoJsonZone.forEach(
        feature ->
            eventProducer.accept(List.of(new FeatureVggRequested(detectionIdentifier, feature))));
    return withStepStatistics(detection);
  }
}
