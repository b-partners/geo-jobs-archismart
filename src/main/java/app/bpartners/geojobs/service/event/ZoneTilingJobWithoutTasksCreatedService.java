package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobWithoutTasksCreated;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.DuplicatedTilingJob;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.TilingJobDuplicatedMailer;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ZoneTilingJobWithoutTasksCreatedService
    implements Consumer<ZoneTilingJobWithoutTasksCreated> {
  private final TilingTaskRepository taskRepository;
  private final ZoneTilingJobService jobService;
  private final TilingJobDuplicatedMailer tilingJobDuplicatedMailer;

  @Override
  @Transactional
  public void accept(ZoneTilingJobWithoutTasksCreated zoneTilingJobWithoutTasksCreated) {
    ZoneTilingJob originalJob = zoneTilingJobWithoutTasksCreated.getOriginalJob();
    String jobId = originalJob.getId();
    String duplicatedJobId = zoneTilingJobWithoutTasksCreated.getDuplicatedJobId();
    List<ParcelTilingTask> parcelTilingTasks = taskRepository.findAllByJobId(jobId);
    boolean saveZDJ = true;
    JobStatus newStatus = null;
    ZoneTilingJob duplicatedJob =
        jobService.duplicateWithNewStatus(
            duplicatedJobId, originalJob, parcelTilingTasks, saveZDJ, newStatus);
    tilingJobDuplicatedMailer.accept(new DuplicatedTilingJob(originalJob, duplicatedJob));
  }
}
