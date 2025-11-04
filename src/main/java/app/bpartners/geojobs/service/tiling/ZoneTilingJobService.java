package app.bpartners.geojobs.service.tiling;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.tile.ParcelTilingTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.zone.ImportedZoneTilingJobSaved;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobWithoutTasksCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TilingTaskMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoomMapper;
import app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.FilteredTilingJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFilteredMailer;
import app.bpartners.geojobs.service.NotFinishedTaskRetriever;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.TilingTaskConsumer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import lombok.SneakyThrows;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneTilingJobService extends JobService<ParcelTilingTask, ZoneTilingJob> {
  private final ZoneDetectionJobService detectionJobService;
  private final JobFilteredMailer<ZoneTilingJob> tilingFilteredMailer;
  private final NotFinishedTaskRetriever<ParcelTilingTask> notFinishedTaskRetriever;
  private static ZoomMapper zoomMapper;
  private static TilingTaskMapper tilingTaskMapper;
  private final TilingTaskConsumer tilingTaskConsumer;
  private final Workers workers;

  static {
    FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(null));
    zoomMapper = new ZoomMapper();
    tilingTaskMapper = new TilingTaskMapper(featureMapper);
  }

  public ZoneTilingJobService(
      JpaRepository<ZoneTilingJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskRepository<ParcelTilingTask> taskRepository,
      EventProducer eventProducer,
      ZoneDetectionJobService detectionJobService,
      JobFilteredMailer<ZoneTilingJob> tilingFilteredMailer,
      NotFinishedTaskRetriever<ParcelTilingTask> notFinishedTaskRetriever,
      ZoomMapper zoomMapper,
      TilingTaskMapper tilingTaskMapper,
      TaskStatisticRepository taskStatisticRepository,
      TilingTaskConsumer tilingTaskConsumer,
      Workers workers) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        ZoneTilingJob.class);
    this.detectionJobService = detectionJobService;
    this.tilingFilteredMailer = tilingFilteredMailer;
    this.notFinishedTaskRetriever = notFinishedTaskRetriever;
    this.zoomMapper = zoomMapper;
    this.tilingTaskMapper = tilingTaskMapper;
    this.tilingTaskConsumer = tilingTaskConsumer;
    this.workers = workers;
  }

  public ZoneTilingJob importFromBucket(
      ZoneTilingJob job,
      String bucketName,
      String bucketPathPrefix,
      GeoServerParameter geoServerParameter,
      String geoServerUrl,
      Long startFrom,
      Long endAt,
      BucketSeparatorType bucketSeparator) {
    var createdJob = repository.save(job);
    eventProducer.accept(
        List.of(
            ImportedZoneTilingJobSaved.builder()
                .startFrom(startFrom)
                .endAt(endAt)
                .jobId(createdJob.getId())
                .bucketName(bucketName)
                .bucketPathPrefix(bucketPathPrefix)
                .geoServerParameter(geoServerParameter)
                .geoServerUrl(geoServerUrl)
                .bucketSeparatorType(bucketSeparator)
                .build()));
    return createdJob;
  }

  @Transactional
  public FilteredTilingJob dispatchTasksBySuccessStatus(String jobId) {
    ZoneTilingJob job = findById(jobId);
    if (job.isSucceeded()) {
      throw new BadRequestException(
          "All job(id="
              + jobId
              + ") tasks are SUCCEEDED. "
              + "Use POST /tiling/id/duplications to duplicate job instead");
    }
    List<ParcelTilingTask> parcelTilingTasks = taskRepository.findAllByJobId(jobId);
    List<ParcelTilingTask> succeededTasks =
        parcelTilingTasks.stream().filter(Task::isSucceeded).toList();
    List<ParcelTilingTask> notSucceededTasks =
        parcelTilingTasks.stream().filter(task -> !task.isSucceeded()).toList();
    String succeededJobId = randomUUID().toString();
    String notSucceededJobId = randomUUID().toString();
    var succeededJob =
        duplicateWithNewStatus(
            succeededJobId,
            job,
            succeededTasks,
            true,
            JobStatus.builder()
                .jobId(succeededJobId)
                .progression(FINISHED)
                .health(SUCCEEDED)
                .creationDatetime(now())
                .build());
    var notSucceededJob =
        duplicateWithNewStatus(
            notSucceededJobId,
            job,
            notSucceededTasks,
            false,
            JobStatus.builder()
                .jobId(notSucceededJobId)
                .progression(PENDING)
                .health(UNKNOWN)
                .creationDatetime(now())
                .build());
    FilteredTilingJob filteredTilingJob =
        new FilteredTilingJob(jobId, succeededJob, notSucceededJob);
    tilingFilteredMailer.accept(filteredTilingJob);
    return filteredTilingJob;
  }

  @Transactional
  public ZoneTilingJob retryFailedTask(String jobId) {
    ZoneTilingJob job = findById(jobId);
    List<ParcelTilingTask> parcelTilingTasks = taskRepository.findAllByJobId(jobId);
    if (!parcelTilingTasks.stream()
        .allMatch(task -> task.getStatus().getProgression().equals(FINISHED))) {
      throw new BadRequestException("Only job with all finished tasks can be retry");
    }
    List<ParcelTilingTask> failedTasks =
        parcelTilingTasks.stream()
            .filter(task -> task.getStatus().getHealth().equals(FAILED))
            .toList();
    if (failedTasks.isEmpty()) {
      throw new BadRequestException(
          "All tilling tasks of job(id=" + jobId + ") are already SUCCEEDED");
    }
    List<ParcelTilingTask> savedFailedTasks =
        taskRepository.saveAll(failedTasks.stream().map(notFinishedTaskRetriever).toList());
    savedFailedTasks.forEach(
        task -> eventProducer.accept(List.of(new ParcelTilingTaskCreated(task))));
    // /!\ Force job status to status PROCESSING again
    job.hasNewStatus(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(TILING)
            .progression(PROCESSING)
            .health(RETRYING)
            .creationDatetime(now())
            .build());
    return repository.save(job);
  }

  @Transactional
  @Override
  public ZoneTilingJob create(ZoneTilingJob job, List<ParcelTilingTask> tasks) {
    if (job.isRooferMade()) {
      return super.create(job, tasks);
    }

    var saved = super.create(job, tasks);
    eventProducer.accept(List.of(new ZoneTilingJobCreated(saved)));
    eventProducer.accept(List.of(new ZTJStatusRecomputingSubmitted(saved.getId())));
    return saved;
  }

  @Transactional
  public void fireTasks(ZoneTilingJob job) {
    getTasks(job).forEach(task -> eventProducer.accept(List.of(new ParcelTilingTaskCreated(task))));
  }

  public List<ParcelTilingTask> consumeTasks(String jobId) {
    var job = findById(jobId);
    var tasks = getTasks(job);

    List<Callable<Void>> callables =
        tasks.stream()
            .map(
                task ->
                    (Callable<Void>)
                        () -> {
                          tilingTaskConsumer.accept(task);
                          return null;
                        })
            .toList();

    workers.invokeAll(callables);

    return tasks;
  }

  @Override
  protected void onStatusChanged(ZoneTilingJob oldJob, ZoneTilingJob newJob) {
    eventProducer.accept(
        List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }

  @Transactional
  public ZoneTilingJob duplicate(String jobId) {
    String duplicatedJobId = randomUUID().toString();
    ZoneTilingJob originalJob = findById(jobId);

    eventProducer.accept(
        List.of(
            ZoneTilingJobWithoutTasksCreated.builder()
                .duplicatedJobId(duplicatedJobId)
                .originalJob(originalJob)
                .build()));

    return originalJob.duplicate(duplicatedJobId);
  }

  public ZoneTilingJob duplicateWithNewStatus(
      String duplicatedJobId,
      ZoneTilingJob job,
      List<ParcelTilingTask> tasks,
      boolean saveZDJ,
      JobStatus newStatus) {
    List<ParcelTilingTask> duplicatedTasks =
        tasks.stream()
            .map(
                task -> {
                  var newTaskId = randomUUID().toString();
                  var newParcelId = randomUUID().toString();
                  var newParcelContentId = randomUUID().toString();
                  boolean hasSameStatuses = false;
                  boolean hasSameTile = false;
                  return task.duplicate(
                      newTaskId,
                      duplicatedJobId,
                      newParcelId,
                      newParcelContentId,
                      hasSameStatuses,
                      hasSameTile);
                })
            .toList();
    ZoneTilingJob jobToDuplicate = job.duplicate(duplicatedJobId);
    if (newStatus != null) {
      List<JobStatus> statusHistory = new ArrayList<>(jobToDuplicate.getStatusHistory());
      statusHistory.add(newStatus);
      jobToDuplicate.setStatusHistory(statusHistory);
    }
    ZoneTilingJob duplicatedJob = repository.save(jobToDuplicate);
    taskRepository.saveAll(duplicatedTasks);
    if (saveZDJ) {
      detectionJobService.saveZDJFromZTJ(duplicatedJob);
    }
    return duplicatedJob;
  }

  @SneakyThrows
  public static List<ParcelTilingTask> getTilingTasks(CreateZoneTilingJob job, String jobId) {
    var serverUrl = new URI(Objects.requireNonNull(job.getGeoServerUrl())).toURL();
    return Objects.requireNonNull(job.getFeatures()).stream()
        .map(
            feature -> {
              feature
                  .getProperties()
                  .put(
                      "zoom",
                      zoomMapper
                          .toDomain(Objects.requireNonNull(job.getZoomLevel()))
                          .getZoomLevel());
              return tilingTaskMapper.from(feature, serverUrl, job.getGeoServerParameter(), jobId);
            })
        .collect(toList());
  }
}
