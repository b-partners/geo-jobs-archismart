package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import org.junit.jupiter.api.Test;

public class TileDetectionTaskSucceededServiceTest {
  TileDetectionTaskStatusService tileDetectionTaskStatusServiceMock = mock();
  TileDetectionTaskRepository tileDetectionTaskRepositoryMock = mock();
  TileDetectionTaskSucceededService subject =
      new TileDetectionTaskSucceededService(
          tileDetectionTaskStatusServiceMock, tileDetectionTaskRepositoryMock);

  @Test
  void consume_with_any_error_ok() {
    when(tileDetectionTaskRepositoryMock.save(any())).thenReturn(new TileDetectionTask());
    when(tileDetectionTaskStatusServiceMock.succeed(any())).thenReturn(new TileDetectionTask());

    assertDoesNotThrow(
        () ->
            subject.accept(
                new TileDetectionTaskSucceeded(new TileDetectionTask(), any(String.class))));
  }
}
