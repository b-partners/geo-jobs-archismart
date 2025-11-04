package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobType.REVIEWING;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.GeoJobType.ANNOTATION_DELIVERY;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnnotationService {
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
  private final TaskExtractor taskExtractor;
  private final LabelConverter labelConverter;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final AnnotationDeliveryJobService annotationDeliveryJobService;
  private final AdminApi adminApi;
  private final JobsApi jobsApi;
  private static final int DEFAULT_IMAGES_HEIGHT = 1024;
  private static final int DEFAULT_IMAGES_WIDTH = 1024;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final BucketComponent bucketComponent;

  public AnnotationService(
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelConverter labelConverter,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      AnnotationDeliveryJobService annotationDeliveryJobService,
      DetectableObjectConfigurationRepository objectConfigurationRepository,
      BucketComponent bucketComponent) {
    this.adminApi = new AdminApi(annotatorApiConf.newApiClientWithApiKey());
    this.jobsApi = new JobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelConverter = labelConverter;
    this.labelExtractor = labelExtractor;
    this.annotatorUserInfoGetter = annotatorUserInfoGetter;
    this.annotationDeliveryJobService = annotationDeliveryJobService;
    this.objectConfigurationRepository = objectConfigurationRepository;
    this.bucketComponent = bucketComponent;
  }

  public Job getAnnotationJobById(String annotationJobId) {
    try {
      return jobsApi.getJob(annotationJobId);
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }

  @SneakyThrows
  public void createAnnotationJob(
      HumanDetectionJob humanDetectionJob,
      String jobName,
      List<MachineDetectedTile> machineDetectedTiles) {
    var detectableObjectConfigurations = humanDetectionJob.getDetectableObjectConfigurations();
    var expectedLabels =
        retrieveExpectedLabelsFromObjectConfiguration(detectableObjectConfigurations);
    var annotatedTasks =
        taskExtractor.apply(
            machineDetectedTiles, annotatorUserInfoGetter.getUserId(), expectedLabels);
    var extractLabelsFromTasks = labelExtractor.extractLabelsFromTasks(annotatedTasks);
    var labels = extractLabelsFromTasks.isEmpty() ? expectedLabels : extractLabelsFromTasks;
    var annotationJobId = humanDetectionJob.getAnnotationJobId();
    var detectionJobId = humanDetectionJob.getZoneDetectionJobId();
    var annotationDeliveryJobId = randomUUID().toString();

    var annotationDeliveryJob =
        fromDetectionJob(annotationDeliveryJobId, annotationJobId, jobName, detectionJobId, labels);
    var annotationDeliveryTasks =
        annotatedTasks.stream()
            .map(
                annotatedTask ->
                    fromAnnotatedTask(annotationDeliveryJobId, annotationJobId, annotatedTask))
            .collect(Collectors.toList());
    log.info("DEBUG annotationDeliveryJobId={} for jobName={}", annotationDeliveryJobId, jobName);
    log.info(
        "DEBUG annotationDeliveryJob.id={} for jobName={}", annotationDeliveryJob.getId(), jobName);

    annotationDeliveryJobService.create(annotationDeliveryJob, annotationDeliveryTasks);
  }

  @NonNull
  private List<Label> retrieveExpectedLabelsFromObjectConfiguration(
      List<DetectableObjectConfiguration> detectableObjects) {
    return detectableObjects.stream()
        .map(object -> labelConverter.apply(object.getObjectType()))
        .toList();
  }

  private AnnotationDeliveryTask fromAnnotatedTask(
      String annotationDeliveryJobId,
      String annotationJobId,
      CreateAnnotatedTask createAnnotatedTask) {
    var annotationDeliveryTaskId = randomUUID().toString();
    var annotatedTaskId = createAnnotatedTask.getId();
    return AnnotationDeliveryTask.builder()
        .id(annotationDeliveryTaskId)
        .jobId(annotationDeliveryJobId)
        .annotationJobId(annotationJobId)
        .annotationTaskId(annotatedTaskId)
        .createAnnotatedTask(createAnnotatedTask)
        .submissionInstant(now())
        .statusHistory(
            List.of(
                app.bpartners.geojobs.job.model.TaskStatus.builder()
                    .id(randomUUID().toString())
                    .taskId(annotationDeliveryTaskId)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .creationDatetime(now())
                    .build()))
        .build();
  }

  @NonNull
  private AnnotationDeliveryJob fromDetectionJob(
      String annotationDeliveryJobId,
      String annotationJobId,
      String annotationJobName,
      String detectionJobId,
      List<Label> labels) {
    var annotationDeliveryJob =
        AnnotationDeliveryJob.builder()
            .id(annotationDeliveryJobId)
            .annotationJobId(annotationJobId)
            .annotationJobName(annotationJobName)
            .detectionJobId(detectionJobId)
            .labels(labels)
            .submissionInstant(now())
            .build();
    annotationDeliveryJob.hasNewStatus(
        app.bpartners.geojobs.job.model.JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(annotationDeliveryJobId)
            .creationDatetime(now())
            .jobType(ANNOTATION_DELIVERY)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return annotationDeliveryJob;
  }

  public void createAnnotationJob(HumanDetectionJob humanDetectionJob) throws ApiException {
    createAnnotationJob(
        humanDetectionJob, "geo-jobs" + now(), humanDetectionJob.getMachineDetectedTiles());
  }

  public void addAnnotationTask(String jobId, CreateAnnotatedTask annotatedTask) {
    try {
      adminApi.addAnnotatedTasksToAnnotatedJob(jobId, List.of(annotatedTask));
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }

  public List<AnnotationBatch> getAnnotations(String annotationJobId, String taskId) {
    try {
      int minPage = 1;
      int maxPageSize = 500;
      return adminApi.getAnnotationBatchesByJobTask(annotationJobId, taskId, minPage, maxPageSize);
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  public List<AnnotationRetrievingTask> retrieveTasksFromAnnotationJob(
      String humanZDJId,
      String retrievingJobId,
      String annotationJobId,
      Integer page,
      Integer pageSize,
      String userId) {
    List<Task> annotationTasks;
    try {
      annotationTasks =
          adminApi.getJobTasks(
              annotationJobId,
              page,
              pageSize,
              null, // So get any tasks either the status is
              userId); // page, pageSize and UserId not required
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
    return annotationTasks.stream()
        .map(
            annotationTask -> {
              var metadata = getTileMetaData(annotationTask.getFilename());
              var zoom = metadata.getFirst();
              var xTile = metadata.get(metadata.size() - 2);
              var yTile = metadata.getLast();
              // TODO: avoid redundant persisted metadata (x - y - z)
              return AnnotationRetrievingTask.builder()
                  .id(randomUUID().toString())
                  .jobId(retrievingJobId)
                  .annotationTaskId(annotationTask.getId())
                  .annotationJobId(annotationJobId)
                  .statusHistory(List.of())
                  .submissionInstant(now())
                  .humanZoneDetectionJobId(humanZDJId)
                  .zoom(zoom)
                  .xTile(xTile)
                  .yTile(yTile)
                  .build();
            })
        .collect(Collectors.toList());
  }

  public void saveAnnotationJob(
      String detectionJobId,
      String annotationJobId,
      String annotationJobName,
      List<Label> labels,
      JobStatus annotationJobStatus) {
    var defaultBucketName = bucketComponent.getBucketName();
    var bucketName = new AtomicReference<>(defaultBucketName);
    var detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(detectionJobId);
    detectableObjectConfigurations.stream()
        .filter(objectConfiguration -> objectConfiguration.getBucketStorageName() != null)
        .findFirst()
        .ifPresent(
            objectConfiguration -> {
              bucketName.set(objectConfiguration.getBucketStorageName());
            });

    try {
      jobsApi.saveJob(
          annotationJobId,
          new CrupdateJob()
              .id(annotationJobId)
              .name(annotationJobName)
              .bucketName(bucketName.get())
              .folderPath(null)
              .labels(labels)
              .ownerEmail("tech@birdia.fr")
              .status(annotationJobStatus)
              .type(REVIEWING)
              .imagesHeight(DEFAULT_IMAGES_HEIGHT)
              .imagesWidth(DEFAULT_IMAGES_WIDTH)
              .teamId(annotatorUserInfoGetter.getTeamId()));
    } catch (java.lang.Exception e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          SERVER_EXCEPTION,
          "Fail to delivery annotationJob(id="
              + annotationJobId
              + "status=PENDING) causing retry,"
              + " exception message="
              + e.getMessage());
    }
  }

  private List<Integer> getTileMetaData(String filename) {
    var metadata = new ArrayList<Integer>();
    var filenameMatcher = DIGIT_PATTERN.matcher(filename);
    while (filenameMatcher.find()) {
      metadata.add(new BigInteger(filenameMatcher.group()).intValue());
    }
    int size = metadata.size();
    return metadata.subList(size - 3, size);
  }
}
