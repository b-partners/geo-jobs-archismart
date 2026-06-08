package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.SuccessStatus.NOT_SUCCEEDED;
import static app.bpartners.geojobs.endpoint.rest.model.SuccessStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TilingTaskMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoomMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.model.TiledParcel;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneTilingJobValidator;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.service.ParcelService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class ZoneTilingController {
  private final ZoneTilingJobService service;
  private final ParcelService parcelService;
  private final ZoneTilingJobMapper mapper;
  private final ZoomMapper zoomMapper;
  private final TilingTaskMapper tilingTaskMapper;
  private final TaskStatisticMapper taskStatisticMapper;
  private final ZoneTilingJobValidator zoneTilingJobValidator;
  private final StatusMapper<JobStatus> jobStatusMapper;
  private final EventProducer eventProducer;
  private final TilingTaskRepository tilingTaskRepository;

  @PostMapping("/tilingJobs/import")
  public ZoneTilingJob importZTJ(@RequestBody ImportZoneTilingJob importZoneTilingJob) {
    zoneTilingJobValidator.accept(importZoneTilingJob);
    var job = mapper.toDomain(importZoneTilingJob.getCreateZoneTilingJob(), false);
    var bucketName = importZoneTilingJob.getBucketName();
    var bucketPathPrefix = importZoneTilingJob.getBucketPathPrefix();
    var geoServerParameter = importZoneTilingJob.getCreateZoneTilingJob().getGeoServerParameter();
    var geoServerUrl = importZoneTilingJob.getCreateZoneTilingJob().getGeoServerUrl();
    var startFrom =
        importZoneTilingJob.getStartFrom() == null
            ? null
            : importZoneTilingJob.getStartFrom().longValue();
    var endAt =
        importZoneTilingJob.getEndAt() == null ? null : importZoneTilingJob.getEndAt().longValue();
    var bucketSeparator = importZoneTilingJob.getBucketSeparator();
    return mapper.toRest(
        service.importFromBucket(
            job,
            bucketName,
            bucketPathPrefix,
            geoServerParameter,
            geoServerUrl,
            startFrom,
            endAt,
            bucketSeparator),
        List.of());
  }

  @GetMapping("/tilingJobs/{id}/recomputedStatus")
  public Status getZTJRecomputedStatus(@PathVariable String id) {
    var tilingJob = service.findById(id);
    JobStatus jobStatus = tilingJob.getStatus();
    if (!jobStatus.getProgression().equals(FINISHED)) {
      eventProducer.accept(List.of(new ZTJStatusRecomputingSubmitted(id)));
    }
    return jobStatusMapper.toRest(jobStatus);
  }

  @GetMapping("/tilingJobs/{id}/taskStatistics")
  public TaskStatistic getTilingTaskStatistics(@PathVariable String id) {
    return taskStatisticMapper.toRest(service.computeTaskStatistics(id));
  }

  @PostMapping("/tilingJobs")
  public ZoneTilingJob tileZone(@RequestBody CreateZoneTilingJob createJob) {
    var job = mapper.toDomain(createJob, false);
    var tilingTasks = service.getTilingTasks(createJob, job.getId());
    return mapper.toRest(service.create(job, tilingTasks), tilingTasks);
  }

  @PostMapping("/tilingJobs/{id}/taskFiltering")
  public List<FilteredTilingJob> filterTilingTasks(@PathVariable String id) {
    var filteredTilingJob = service.dispatchTasksBySuccessStatus(id);
    return List.of(
        new FilteredTilingJob()
            .status(SUCCEEDED)
            .job(mapper.toRest(filteredTilingJob.getSucceededJob(), List.of())),
        new FilteredTilingJob()
            .status(NOT_SUCCEEDED)
            .job(mapper.toRest(filteredTilingJob.getNotSucceededJob(), List.of())));
  }

  @PostMapping("/tilingJobs/{id}/duplications")
  public ZoneTilingJob duplicateTilingJob(@PathVariable String id) {
    boolean jobNotSaved = true;
    return mapper.toRest(service.duplicate(id), List.of(), jobNotSaved);
  }

  @PutMapping("/tilingJobs/{id}/retry")
  public ZoneTilingJob processFailedTilingJob(@PathVariable String id) {
    return mapper.toRest(service.retryFailedTask(id), tilingTaskRepository.findAllByJobId(id));
  }

  @GetMapping("/tilingJobs")
  public List<ZoneTilingJob> getTilingJobs(
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "30") BoundedPageSize pageSize) {
    return service.findAll(page, pageSize).stream()
        .map(job -> mapper.toRest(job, tilingTaskRepository.findAllByJobId(job.getId())))
        .toList();
  }

  @GetMapping("/tilingJobs/{id}/parcels")
  public List<TiledParcel> getZTJParcels(@PathVariable("id") String jobId) {
    return parcelService.getParcelsByJobId(jobId).stream().map(tilingTaskMapper::toRest).toList();
  }
}
