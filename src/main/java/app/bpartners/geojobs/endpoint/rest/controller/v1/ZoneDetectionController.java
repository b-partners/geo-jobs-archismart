package app.bpartners.geojobs.endpoint.rest.controller.v1;

import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.*;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.ParcelService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.validator.ZoneDetectionJobValidator;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@V1RestController
@AllArgsConstructor
@Slf4j
public class ZoneDetectionController {
  private final ParcelService parcelService;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final ZoneDetectionJobService service;
  private final ZoneDetectionJobMapper mapper;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;
  private final DetectionTaskMapper taskMapper;
  private final ZoneDetectionJobValidator jobValidator;
  private final TaskStatisticMapper taskStatisticMapper;
  private final StatusMapper<JobStatus> jobStatusMapper;
  private final EventProducer eventProducer;
  private final GeoJsonConversionJobService geoJsonConversionJobService;

  @PostMapping("/detectionJobs/{id}/succeed")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob succeedJob(
      @PathVariable String id) {
    var zoneDetectionJob = service.findById(id);
    if (zoneDetectionJob.isSucceeded()) {
      eventProducer.accept(
          Collections.singleton(ZoneDetectionJobSucceeded.builder().succeededJobId(id).build()));
      var objectConfigurations =
          objectConfigurationRepository.findAllByDetectionJobId(zoneDetectionJob.getId()).stream()
              .map(objectConfigurationMapper::toRest)
              .toList();
      return mapper.toRest(zoneDetectionJob, objectConfigurations);
    }
    throw new BadRequestException("Zone detection on status : " + zoneDetectionJob.getStatus());
  }

  @GetMapping("/detectionJobs/{id}/recomputedParcelsStatuses")
  public Status getZDJTasksRecomputedStatus(@PathVariable String id) {
    var detectionJob = service.findById(id);
    JobStatus jobStatus = detectionJob.getStatus();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZDJParcelsStatusRecomputingSubmitted(id)));
    }
    return jobStatusMapper.toRest(jobStatus);
  }

  @GetMapping("/detectionJobs/{id}/recomputedStatus")
  public Status getZDJRecomputedStatus(@PathVariable String id) {
    var detectionJob = service.findById(id);
    JobStatus jobStatus = detectionJob.getStatus();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(id)));
    }
    return jobStatusMapper.toRest(jobStatus);
  }

  @GetMapping("/detectionJobs/{id}/taskStatistics")
  public TaskStatistic getDetectionTaskStatistics(@PathVariable String id) {
    return taskStatisticMapper.toRest(service.computeTaskStatistics(id));
  }

  @PostMapping("/detectionJobs/{annotationJobId}/humanVerificationStatus")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob checkHumanDetectionJobStatus(
      @PathVariable String annotationJobId) {
    var job = service.checkHumanDetectionJobStatus(annotationJobId);
    var objectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(job.getId()).stream()
            .map(objectConfigurationMapper::toRest)
            .toList();
    return mapper.toRest(job, objectConfigurations);
  }

  @GetMapping("/detectionJobs/{id}/detectedParcels")
  public List<DetectedParcel> getZDJParcels(@PathVariable(name = "id") String detectionJobId) {
    return parcelService.getParcelsByJobId(detectionJobId).stream()
        .map(parcel -> taskMapper.toRest(detectionJobId, parcel))
        .toList();
  }

  @GetMapping("/detectionJobs")
  public List<app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob> getDetectionJobs(
      @RequestParam PageFromOne page, @RequestParam BoundedPageSize pageSize) {
    return service.findAll(page, pageSize).stream()
        .map(
            zdj -> {
              var jobId = zdj.getId();
              var objectConfigurations =
                  objectConfigurationRepository.findAllByDetectionJobId(jobId).stream()
                      .map(objectConfigurationMapper::toRest)
                      .toList();
              return mapper.toRest(zdj, objectConfigurations);
            })
        .toList();
  }

  @PostMapping("/detectionJobs/{id}/process")
  public app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob processZDJ(
      @PathVariable("id") String jobId,
      @RequestBody List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    jobValidator.accept(jobId);
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration>
        configurations =
            detectableObjectConfigurations.stream()
                .map(objectConf -> objectConfigurationMapper.toDomain(jobId, objectConf))
                .toList();
    ZoneDetectionJob processedZDJ = service.fireTasks(jobId, configurations);
    return mapper.toRest(processedZDJ, detectableObjectConfigurations);
  }

  @GetMapping("/detectionJobs/{id}/geojsonsUrl")
  public GeoJsonsUrl getZDJGeojsonsUrl(@PathVariable(value = "id") String detectionJobId) {
    return geoJsonConversionJobService.getOrComputeGeoJsonUrl(detectionJobId);
  }
}
