package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationModelDeliveryRequested;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.HumanDetectionJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationModelDeliveryRequestedService<T extends AnnotationModelDeliveryRequested>
    implements Consumer<T> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final HumanDetectionJobService humanDetectionJobService;
  private final EventProducer eventProducer;

  @Override
  public void accept(T event) {
    var annotationJobId = event.getAnnotationJobId();
    var zoneDetectionJobId = event.getZoneDetectionJobId();
    var detectableObjectConfigurations = event.getDetectableObjectConfigurations();
    var minimumConfidenceForDelivery = event.getMinimumConfidenceForDelivery();
    var humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJobId);
    var machineDetectedTiles = getMachineDetectedTiles(event, zoneDetectionJobId);

    var humanDetectionJobId = randomUUID().toString();
    var savedHumanJob =
        humanDetectionJobService.create(
            annotationJobId,
            humanDetectionJobId,
            machineDetectedTiles,
            humanZDJ.getId(),
            detectableObjectConfigurations);
    if (machineDetectedTiles.isEmpty()) {
      log.warn(
          "No potential "
              + event.getAnnotationModelDeliveryType()
              + " objects found from ZDJ(id="
              + zoneDetectionJobId
              + ")");
    } else {
      var jobName =
          getJobName(
              humanZDJ, savedHumanJob, machineDetectedTiles, minimumConfidenceForDelivery, event);
      eventProducer.accept(List.of(new HumanDetectionJobCreated(savedHumanJob.getId(), jobName)));
    }
  }

  @NonNull
  private String getJobName(
      ZoneDetectionJob humanZDJ,
      HumanDetectionJob savedHumanJob,
      List<MachineDetectedTile> machineDetectedTiles,
      Double minimumConfidenceForDelivery,
      T event) {
    LocalDateTime dateTime = LocalDateTime.ofInstant(now(), ZoneId.of("Europe/Paris"));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    switch (event.getAnnotationModelDeliveryType()) {
      case TRUE_POSITIVE -> {
        return humanZDJ.getZoneName()
            + " - "
            + savedHumanJob.getDetectableObjectConfigurations().stream()
                .map(DetectableObjectConfiguration::getObjectType)
                .toList()
            + " "
            + machineDetectedTiles.size()
            + " images with object detection confidence >= "
            + minimumConfidenceForDelivery * 100
            + "%"
            + " "
            + dateTime.format(formatter);
      }
      case FALSE_POSITIVE -> {
        return humanZDJ.getZoneName()
            + " - "
            + savedHumanJob.getDetectableObjectConfigurations().stream()
                .map(DetectableObjectConfiguration::getObjectType)
                .toList()
            + " "
            + machineDetectedTiles.size()
            + " images with object detection confidence < "
            + minimumConfidenceForDelivery * 100
            + "%"
            + " "
            + dateTime.format(formatter);
      }
      case WITHOUT_DETECTED_OBJECT -> {
        return humanZDJ.getZoneName()
            + " - "
            + savedHumanJob.getDetectableObjectConfigurations().stream()
                .map(DetectableObjectConfiguration::getObjectType)
                .toList()
            + " "
            + machineDetectedTiles.size()
            + " images without object "
            + minimumConfidenceForDelivery * 100
            + " "
            + dateTime.format(formatter);
      }
    }
    throw new IllegalArgumentException(
        "Unknown AnnotationModelDeliveryType " + event.getAnnotationModelDeliveryType());
  }

  private List<MachineDetectedTile> findAllInDoubtByZdjJobIdGreaterThanOrEquals(
      String zoneDetectionJobId, double minConfidenceForDelivery) {
    return machineDetectedTileRepository.findAllInDoubtByZdjJobId(
        zoneDetectionJobId, minConfidenceForDelivery, true);
  }

  private List<MachineDetectedTile> findAllInDoubtByZdjJobIdLessThan(
      String zoneDetectionJobId, double minConfidenceForDelivery) {
    return machineDetectedTileRepository.findAllInDoubtByZdjJobId(
        zoneDetectionJobId, minConfidenceForDelivery, false);
  }

  private List<MachineDetectedTile> getMachineDetectedTiles(T event, String zoneDetectionJobId) {
    var minConfidenceForDelivery = event.getMinimumConfidenceForDelivery();
    return switch (event.getAnnotationModelDeliveryType()) {
      case TRUE_POSITIVE ->
          findAllInDoubtByZdjJobIdGreaterThanOrEquals(zoneDetectionJobId, minConfidenceForDelivery);
      case FALSE_POSITIVE ->
          findAllInDoubtByZdjJobIdLessThan(zoneDetectionJobId, minConfidenceForDelivery);
      case WITHOUT_DETECTED_OBJECT ->
          machineDetectedTileRepository.findAllInDoubtTilesWithoutObjectByZdjJobId(
              zoneDetectionJobId);
    };
  }
}
