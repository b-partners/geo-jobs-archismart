package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTrackingRegistrationRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.BucketObjectCopier;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import app.bpartners.geojobs.repository.model.detection.DetectionFileType;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.detection.inference.InferenceSignatureComputer;
import app.bpartners.geojobs.service.event.DetectionSucceededService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetectionReuseService {
  private static final int MAX_CANDIDATES = 10;
  private static final int MAX_NEARBY_CANDIDATES = 20;
  private static final double NEARBY_BOUNDS_MARGIN = 1.01;
  private static final String DEFAULT_FOLDER = "reused";
  private static final String ZONE_IMAGES_FOLDER = "zone_images/";
  private static final Set<DetectionFileType> REUSED_FILE_TYPES =
      Set.of(
          DetectionFileType.ASSEMBLE_VGG,
          DetectionFileType.ASSEMBLE_IMAGE,
          DetectionFileType.ANNOTATED_ASSEMBLE_IMAGE,
          DetectionFileType.GEOJSON);

  private final DetectionRepository detectionRepository;
  private final InferenceSignatureComputer inferenceSignatureComputer;
  private final BucketObjectCopier bucketObjectCopier;
  private final BucketComponent bucketComponent;
  private final CustomBucketComponent customBucketComponent;
  private final Workers workers;
  private final EventProducer eventProducer;
  private final DetectionSucceededService detectionSucceededService;
  private final ZoneSimilarityConf zoneSimilarityConf;

  public DetectionReuseFingerprint fingerprintOf(Detection detection) {
    String zoneHash = null;
    String inferenceSignature = null;
    ZoneBounds zoneBounds = null;
    try {
      zoneHash =
          AnalysisProjectKeys.zoneHashOfIndividual(detection.getDomainProvidedGeoJsonZone())
              .orElse(null);
      if (zoneHash != null) {
        zoneBounds =
            ZoneGeometry.ofIndividual(detection.getDomainProvidedGeoJsonZone())
                .map(ZoneGeometry::bounds)
                .orElse(null);
        inferenceSignature =
            inferenceSignatureComputer
                .compute(
                    detection.getDetectableObjectModels(),
                    detection.getDetectableObjectConfigurations(),
                    detectionResolution(detection))
                .orElse(null);
      }
    } catch (RuntimeException e) {
      log.warn(
          "Could not compute the reuse fingerprint of detection(e2Id={})",
          detection.getEndToEndId(),
          e);
    }
    return new DetectionReuseFingerprint(zoneHash, inferenceSignature, zoneBounds);
  }

  public Optional<Detection> reuseResultsOf(Detection target) {
    if (!isEligible(target)) {
      return Optional.empty();
    }
    var copiedKeys = new CopyOnWriteArrayList<String>();
    try {
      var source = findSource(target);
      if (source.isEmpty()) {
        return Optional.empty();
      }
      var copied = copyResults(source.get(), target, copiedKeys);
      var saved = detectionRepository.save(copied);
      log.info(
          "Detection(e2Id={}) reuses the results of detection(id={})",
          target.getEndToEndId(),
          source.get().getId());
      notifyCompletion(saved);
      return Optional.of(saved);
    } catch (RuntimeException e) {
      log.warn(
          "Could not reuse previous results for detection(e2Id={}), processing it instead",
          target.getEndToEndId(),
          e);
      copiedKeys.forEach(bucketObjectCopier::deleteQuietly);
      return Optional.empty();
    }
  }

  private static boolean isEligible(Detection target) {
    return target.getCommunityOwnerId() != null
        && target.getZoneHash() != null
        && target.getInferenceSignature() != null
        && target.getGeojsonS3FileKey() == null
        && target.getZtjId() == null
        && !target.isStillOnConfiguringStep();
  }

  private Optional<Detection> findSource(Detection target) {
    var candidateIds =
        detectionRepository.findReusableSourceIds(
            target.getCommunityOwnerId(),
            target.getZoneHash(),
            target.getInferenceSignature(),
            target.isIntegrationTest(),
            target.getId(),
            PageRequest.of(0, MAX_CANDIDATES));
    for (var candidateId : candidateIds) {
      var candidate = detectionRepository.findById(candidateId);
      if (candidate.isPresent()
          && isCompatible(candidate.get(), target)
          && hasRequiredImage(candidate.get(), target)) {
        return candidate;
      }
    }
    return findNearbySource(target);
  }

  private Optional<Detection> findNearbySource(Detection target) {
    if (!zoneSimilarityConf.isEnabled()) {
      return Optional.empty();
    }
    var targetZone = ZoneGeometry.ofIndividual(target.getDomainProvidedGeoJsonZone());
    if (targetZone.isEmpty()) {
      return Optional.empty();
    }
    var bounds = targetZone.get().bounds();
    var toleranceInMeters = zoneSimilarityConf.getMaxDisplacementInMeters() * NEARBY_BOUNDS_MARGIN;
    var toleranceLat = toleranceInMeters / ZoneGeometry.METERS_PER_DEGREE;
    var toleranceLon =
        toleranceLat / Math.cos(Math.toRadians((bounds.minLat() + bounds.maxLat()) / 2));
    var candidateIds =
        detectionRepository.findReusableSourceIdsNear(
            target.getCommunityOwnerId(),
            target.getInferenceSignature(),
            target.isIntegrationTest(),
            target.getId(),
            bounds.minLon(),
            bounds.minLat(),
            bounds.maxLon(),
            bounds.maxLat(),
            toleranceLon,
            toleranceLat,
            PageRequest.of(0, MAX_NEARBY_CANDIDATES));
    Detection best = null;
    var bestIou = 0.0;
    for (var candidateId : candidateIds) {
      var candidate = detectionRepository.findById(candidateId);
      if (candidate.isEmpty() || !isCompatible(candidate.get(), target)) {
        continue;
      }
      var iou = similarityOf(targetZone.get(), candidate.get());
      if (iou.isPresent()
          && iou.getAsDouble() > bestIou
          && hasRequiredImage(candidate.get(), target)) {
        best = candidate.get();
        bestIou = iou.getAsDouble();
      }
    }
    if (best != null) {
      log.info(
          "Detection(e2Id={}) matches the zone of detection(id={}) with iou={}",
          target.getEndToEndId(),
          best.getId(),
          bestIou);
    }
    return Optional.ofNullable(best);
  }

  private OptionalDouble similarityOf(ZoneGeometry targetZone, Detection candidate) {
    var candidateZone = ZoneGeometry.ofIndividual(candidate.getDomainProvidedGeoJsonZone());
    if (candidateZone.isEmpty()) {
      return OptionalDouble.empty();
    }
    var iou = targetZone.iouWith(candidateZone.get());
    if (iou < zoneSimilarityConf.getMinIou()
        || !targetZone.isWithinDisplacementOf(
            candidateZone.get(), zoneSimilarityConf.getMaxDisplacementInMeters())) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(iou);
  }

  static boolean isCompatible(Detection source, Detection target) {
    var layer = layerOf(target);
    return layer != null
        && layer.equals(layerOf(source))
        && Objects.equals(source.spatialResolutionInCm(), target.spatialResolutionInCm())
        && objectsOf(source).equals(objectsOf(target))
        && source.getGeoJsonDelimitationType() == target.getGeoJsonDelimitationType()
        && source.isOutputZipped() == target.isOutputZipped();
  }

  private boolean hasRequiredImage(Detection source, Detection target) {
    return !target.needsImageOutput()
        || source.getImageFileKey() != null
        || !zoneImageKeysToCopy(source, target).isEmpty();
  }

  private Map<String, String> zoneImageKeysToCopy(Detection source, Detection target) {
    var existingKeys =
        customBucketComponent
            .listObjects(bucketComponent.getBucketName(), ZONE_IMAGES_FOLDER + source.getId())
            .stream()
            .map(S3Object::key)
            .collect(Collectors.toSet());
    var candidates = new LinkedHashMap<String, String>();
    candidates.put(
        ZONE_IMAGES_FOLDER + source.getId() + ".jpg", ZONE_IMAGES_FOLDER + target.getId() + ".jpg");
    var sourceFeatureKey = featureImageKeyOf(source);
    var targetFeatureKey = featureImageKeyOf(target);
    if (sourceFeatureKey != null && targetFeatureKey != null) {
      candidates.put(sourceFeatureKey, targetFeatureKey);
    }
    var keysToCopy = new LinkedHashMap<String, String>();
    candidates.forEach(
        (sourceKey, targetKey) -> {
          if (existingKeys.contains(sourceKey)) {
            keysToCopy.put(sourceKey, targetKey);
          }
        });
    return keysToCopy;
  }

  private static String featureImageKeyOf(Detection detection) {
    var features = detection.getProvidedGeoJsonZone();
    if (features == null
        || features.isEmpty()
        || features.getFirst().getProperties() == null
        || features.getFirst().getProperties().get("id") == null
        || detection.getZoneName() == null) {
      return null;
    }
    return ZONE_IMAGES_FOLDER
        + detection.getId()
        + "/"
        + features.getFirst().getProperties().get("id")
        + "/"
        + detection.getZoneName()
        + ".jpg";
  }

  private static String layerOf(Detection detection) {
    var properties = detection.getGeoServerProperties();
    if (properties == null || properties.getGeoServerParameter() == null) {
      return null;
    }
    return properties.getGeoServerParameter().getLayers();
  }

  private static Double detectionResolution(Detection detection) {
    return detection.spatialResolutionInCm();
  }

  private static Set<String> objectsOf(Detection detection) {
    var configurations = detection.getDetectableObjectConfigurations();
    if (configurations == null) {
      return Set.of();
    }
    return configurations.stream()
        .map(DetectionReuseService::objectOf)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String objectOf(DetectableObjectConfiguration configuration) {
    return configuration.getObjectType() + ":" + configuration.getMinConfidenceForDetection();
  }

  private Detection copyResults(Detection source, Detection target, List<String> copiedKeys) {
    var destinationBySourceKey = new LinkedHashMap<String, String>();
    if (target.needsImageOutput()) {
      destinationBySourceKey.putAll(zoneImageKeysToCopy(source, target));
    }
    var geojsonKey =
        destinationOf(source, target, source.getGeojsonS3FileKey(), destinationBySourceKey);
    var vggKey = destinationOf(source, target, source.getVggFileKey(), destinationBySourceKey);
    var imageKey = destinationOf(source, target, source.getImageFileKey(), destinationBySourceKey);
    var fileObjects =
        fileObjectsOf(source).stream()
            .map(
                fileObject ->
                    DetectionFileObject.builder()
                        .id(randomUUID().toString())
                        .detectionIdentifier(target.getId())
                        .bucketKey(
                            destinationOf(
                                source, target, fileObject.getBucketKey(), destinationBySourceKey))
                        .fileName(fileObject.getFileName())
                        .fileType(fileObject.getFileType())
                        .creationDatetime(Instant.now())
                        .build())
            .toList();
    copyObjects(destinationBySourceKey, copiedKeys);
    return target.toBuilder()
        .geojsonS3FileKey(geojsonKey)
        .vggFileKey(vggKey)
        .imageFileKey(imageKey)
        .imageWidth(source.getImageWidth())
        .imageHeight(source.getImageHeight())
        .polygonRoofDelimitation(source.getPolygonRoofDelimitation())
        .parcelDelimitations(source.getParcelDelimitations())
        .roofPropertiesComputationCreationDatetime(
            source.getRoofPropertiesComputationCreationDatetime())
        .fileObjects(fileObjects)
        .sourceDetectionId(source.getId())
        .build();
  }

  private static List<DetectionFileObject> fileObjectsOf(Detection source) {
    if (source.getFileObjects() == null) {
      return List.of();
    }
    return source.getFileObjects().stream()
        .filter(fileObject -> REUSED_FILE_TYPES.contains(fileObject.getFileType()))
        .filter(fileObject -> fileObject.getBucketKey() != null)
        .toList();
  }

  private static String destinationOf(
      Detection source, Detection target, String sourceKey, Map<String, String> destinations) {
    if (sourceKey == null) {
      return null;
    }
    return destinations.computeIfAbsent(sourceKey, key -> destinationKey(source, target, key));
  }

  static String destinationKey(Detection source, Detection target, String sourceKey) {
    var separator = sourceKey.indexOf('/');
    var folder = separator < 0 ? DEFAULT_FOLDER : sourceKey.substring(0, separator);
    var rest = separator < 0 ? sourceKey : sourceKey.substring(separator + 1);
    var targetRest =
        rest.contains(source.getId())
            ? rest.replace(source.getId(), target.getId())
            : target.getId() + "/" + rest;
    return folder + "/" + targetRest;
  }

  private void copyObjects(Map<String, String> destinationBySourceKey, List<String> copiedKeys) {
    List<Callable<Void>> copies =
        destinationBySourceKey.entrySet().stream()
            .<Callable<Void>>map(
                entry ->
                    () -> {
                      bucketObjectCopier.copy(entry.getKey(), entry.getValue());
                      copiedKeys.add(entry.getValue());
                      return null;
                    })
            .toList();
    workers.invokeAll(copies);
  }

  private void notifyCompletion(Detection copied) {
    try {
      if (!copied.isSynchronous()) {
        detectionSucceededService.acceptReused(copied);
      }
      eventProducer.accept(List.of(new DetectionTrackingRegistrationRequested(copied.getId())));
    } catch (RuntimeException e) {
      log.error(
          "Could not notify the completion of reused detection(e2Id={})",
          copied.getEndToEndId(),
          e);
    }
  }
}
