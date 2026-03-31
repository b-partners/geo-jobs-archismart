package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.model.page.BoundedPageSize.MAX_SIZE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionTaskCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GeoJsonConversionJobCreatedService implements Consumer<GeoJsonConversionJobCreated> {
  private final HumanDetectedTileRepository humanDetectedTileRepository;
  private final GeoJsonConversionTaskRepository geoJsonConversionTaskRepository;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final EventProducer eventProducer;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  @Override
  public void accept(GeoJsonConversionJobCreated event) {
    var geoJsonConversionJob = event.getGeoJsonConversionJob();
    var zoneDetectionJobId = geoJsonConversionJob.getZoneDetectionJobId();
    var zoneDetectionJobType = geoJsonConversionJob.getZoneDetectionJobType();
    var tasksCount = new AtomicLong(0L);
    var detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(zoneDetectionJobId);
    detectableObjectConfigurations.forEach(
        detectableObjectConfiguration -> {
          var detectableType = detectableObjectConfiguration.getObjectType();
          var tilesCountByDetectableType =
              computeDetectedTilesCountByDetectableType(
                  zoneDetectionJobType, zoneDetectionJobId, detectableType);
          int pageSize =
              tilesCountByDetectableType == 0
                  ? 0
                  : Math.max(1, (int) Math.ceil((double) tilesCountByDetectableType / MAX_SIZE));

          var conversionTasks =
              getGeoJsonConversionTasksFromJobAndDetectableType(
                  pageSize, geoJsonConversionJob, detectableType);
          var savedConversionTasks = geoJsonConversionTaskRepository.saveAll(conversionTasks);
          tasksCount.getAndAccumulate(savedConversionTasks.size(), Long::sum);
          savedConversionTasks.forEach(
              conversionTask ->
                  eventProducer.accept(List.of(new GeoJsonConversionTaskCreated(conversionTask))));
        });

    if (tasksCount.get() == 0) {
      log.info(
          "Any detected tile found to convert into task for ZoneDetectionJob(id={}, type={}) with"
              + " detectableTypes {}",
          zoneDetectionJobId,
          zoneDetectionJobType,
          detectableObjectConfigurations.stream()
              .map(DetectableObjectConfiguration::getObjectType)
              .toList());
      log.info(
          "Processing GeoJsonConversionAssemblyInitiated event for geoJsonConversionJob.id: {}",
          geoJsonConversionJob.getId());
      eventProducer.accept(
          List.of(new GeoJsonConversionAssemblyInitiated(geoJsonConversionJob.getId())));
      return;
    }
    eventProducer.accept(
        List.of(new GeoJsonConversionJobStatusRecomputingSubmitted(geoJsonConversionJob.getId())));
  }

  private ArrayList<GeoJsonConversionTask> getGeoJsonConversionTasksFromJobAndDetectableType(
      int pageSize, GeoJsonConversionJob geoJsonConversionJob, DetectableType detectableType) {
    var conversionTasks = new ArrayList<GeoJsonConversionTask>();
    for (int pageValue = 0; pageValue < pageSize; pageValue++) {
      var geoJsonConversionTask =
          fromConversionJobAndPage(geoJsonConversionJob, pageValue, detectableType);
      conversionTasks.add(geoJsonConversionTask);
    }
    return conversionTasks;
  }

  private GeoJsonConversionTask fromConversionJobAndPage(
      GeoJsonConversionJob geoJsonConversionJob, int pageValue, DetectableType detectableType) {
    var geoJsonConversionTask =
        GeoJsonConversionTask.builder()
            .id(randomUUID().toString())
            .jobId(geoJsonConversionJob.getId())
            .page(pageValue + 1)
            .detectableType(detectableType)
            .submissionInstant(now())
            .build();
    geoJsonConversionTask.hasNewStatus(
        Status.builder()
            .progression(PENDING)
            .health(UNKNOWN)
            .creationDatetime(now())
            .message(null)
            .build());
    return geoJsonConversionTask;
  }

  private Long computeDetectedTilesCountByDetectableType(
      ZoneDetectionJob.DetectionType zoneDetectionJobType,
      String zoneDetectionJobId,
      DetectableType detectableType) {
    switch (zoneDetectionJobType) {
      case HUMAN -> {
        return humanDetectedTileRepository.countByJobId(zoneDetectionJobId);
      }
      case MACHINE -> {
        return machineDetectedTileRepository.countByZdjJobIdAndDetectableType(
            zoneDetectionJobId, detectableType.name());
      }
      default ->
          throw new IllegalArgumentException("Unknown zoneDetectionType " + zoneDetectionJobType);
    }
  }
}
