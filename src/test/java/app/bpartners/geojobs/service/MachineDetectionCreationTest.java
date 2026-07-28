package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.DetectionMachineDetectionStatisticsComputer;
import app.bpartners.geojobs.service.detection.MachineDetectionCreation;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.validator.ZoneDetectionJobValidator;
import org.junit.jupiter.api.Test;

class MachineDetectionCreationTest {
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  ZoneDetectionJobValidator detectionJobValidatorMock = mock();
  DetectionMachineDetectionStatisticsComputer detectionStatisticsComputerMock = mock();
  TileDuplicationRemover tileDuplicationRemoverMock = mock();
  MachineDetectionCreation subject =
      new MachineDetectionCreation(
          zoneDetectionJobServiceMock,
          detectionJobValidatorMock,
          detectionStatisticsComputerMock,
          tileDuplicationRemoverMock);

  @Test
  void job_validate_and_processed() {
    var detectionMock = mock(Detection.class);
    var zoneTilingJobMock = mock(ZoneTilingJob.class);
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    var restDetectionMock = mock(app.bpartners.geojobs.endpoint.rest.model.Detection.class);
    var zoneTilingJobId = randomUUID().toString();
    var zoneDetectionJobId = randomUUID().toString();

    when(zoneTilingJobMock.getId()).thenReturn(zoneTilingJobId);
    when(zoneDetectionJobMock.getId()).thenReturn(zoneDetectionJobId);
    when(zoneDetectionJobServiceMock.getByTilingJobId(zoneTilingJobMock.getId(), MACHINE))
        .thenReturn(zoneDetectionJobMock);
    when(zoneDetectionJobServiceMock.processZDJ(eq(zoneDetectionJobId), anyList()))
        .thenReturn(zoneDetectionJobMock);
    when(detectionStatisticsComputerMock.apply(detectionMock, zoneDetectionJobId))
        .thenReturn(restDetectionMock);

    var actual = subject.apply(detectionMock, zoneTilingJobMock);

    assertEquals(restDetectionMock, actual);
  }
}
