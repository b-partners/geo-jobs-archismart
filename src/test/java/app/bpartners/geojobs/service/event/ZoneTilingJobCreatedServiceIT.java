package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobCreated;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Disabled("flaky test on CI : flyway launches before postgres")
class ZoneTilingJobCreatedServiceIT extends FacadeIT {
  @Autowired ZoneTilingJobCreatedService subject;
  @Autowired ZoneTilingJobService zoneTilingJobService;
  @MockBean EventProducer eventProducer;
  @MockBean TilingTaskRepository taskRepository;
  @Autowired ZoneTilingJobRepository repository;

  @Test
  void accept() {
    String jobId = randomUUID().toString();
    String taskId = randomUUID().toString();
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(TILING)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    ZoneTilingJob toCreate =
        ZoneTilingJob.builder()
            .id(jobId)
            .zoneName("mock")
            .emailReceiver("mock@gmail.com")
            .statusHistory(statusHistory)
            .build();
    ZoneTilingJob created = repository.save(toCreate);
    ZoneTilingJobCreated createdEventPayload =
        ZoneTilingJobCreated.builder().zoneTilingJob(created).build();
    when(taskRepository.findAllByJobId(created.getId()))
        .thenReturn(
            List.of(
                ParcelTilingTask.builder()
                    .id(taskId)
                    .jobId(jobId)
                    .submissionInstant(now())
                    .parcels(
                        List.of(
                            Parcel.builder()
                                .parcelContent(
                                    ParcelContent.builder().id(randomUUID().toString()).build())
                                .build()))
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(PENDING)
                                .health(UNKNOWN)
                                .taskId(taskId)
                                .creationDatetime(now())
                                .build()))
                    .build()));

    subject.accept(createdEventPayload);
    ZoneTilingJob actualAfterAccept = zoneTilingJobService.findById(created.getId());

    int numberOfFeaturesInJob = 1;
    int numberOfZTJStatusComputingEvent = 1;
    int numberOfAutoTaskStatisticComputingEvent = 1;
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(
            eventProducer,
            times(
                numberOfFeaturesInJob
                    + numberOfZTJStatusComputingEvent
                    + numberOfAutoTaskStatisticComputingEvent))
        .accept(listCaptor.capture());
    List<List> allValues = listCaptor.getAllValues();
    var ztjStatusComputeEvent = ((List<ZTJStatusRecomputingSubmitted>) allValues.get(1)).getFirst();
    var taskStatisticComputingEvent =
        ((List<AutoTaskStatisticRecomputingSubmitted>) allValues.getLast()).getFirst();
    assertEquals(UNKNOWN, actualAfterAccept.getStatus().getHealth());
    assertEquals(PENDING, actualAfterAccept.getStatus().getProgression());
    assertEquals(new ZTJStatusRecomputingSubmitted(created.getId()), ztjStatusComputeEvent);
    assertEquals(new AutoTaskStatisticRecomputingSubmitted(jobId), taskStatisticComputingEvent);
  }
}
