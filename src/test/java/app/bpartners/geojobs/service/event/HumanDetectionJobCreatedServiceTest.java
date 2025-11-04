package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreated;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HumanDetectionJobCreatedServiceTest {
  AnnotationService annotationServiceMock = mock();
  HumanDetectionJobRepository detectionJobRepositoryMock = mock();
  HumanDetectionJobCreatedService subject =
      new HumanDetectionJobCreatedService(annotationServiceMock, detectionJobRepositoryMock);

  @Test
  void accept_ok() {
    var machineDetectedTiles = List.of(new MachineDetectedTile());
    var job = HumanDetectionJob.builder().machineDetectedTiles(machineDetectedTiles).build();
    var jobName = "jobName";
    var humanDetectionJobId = "humanDetectionJobId";
    when(detectionJobRepositoryMock.findById(humanDetectionJobId)).thenReturn(Optional.of(job));

    subject.accept(new HumanDetectionJobCreated(humanDetectionJobId, jobName));

    verify(detectionJobRepositoryMock, times(1)).findById(humanDetectionJobId);
    verify(annotationServiceMock, times(1)).createAnnotationJob(job, jobName, machineDetectedTiles);
  }
}
