package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreated;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HumanDetectionJobCreatedService implements Consumer<HumanDetectionJobCreated> {
  private final AnnotationService annotationService;
  private final HumanDetectionJobRepository humanDetectionJobRepository;

  @Override
  public void accept(HumanDetectionJobCreated humanDetectionJobCreated) {
    var jobId = humanDetectionJobCreated.getHumanDetectionJobId();
    var jobName = humanDetectionJobCreated.getJobName();
    var humanDetectionJob =
        humanDetectionJobRepository
            .findById(jobId)
            .orElseThrow(
                () -> new NotFoundException("HumanDetectionJob(id=" + jobId + ") not found"));

    annotationService.createAnnotationJob(
        humanDetectionJob, jobName, humanDetectionJob.getMachineDetectedTiles());
  }
}
