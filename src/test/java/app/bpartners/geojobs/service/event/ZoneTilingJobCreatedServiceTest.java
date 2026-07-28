package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobCreated;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneTilingJobCreatedServiceTest {
  ZoneTilingJobService zoneTilingJobServiceMock = mock(ZoneTilingJobService.class);
  EventProducer eventProducerMock = mock(EventProducer.class);
  ZoneTilingJobCreatedService subject =
      new ZoneTilingJobCreatedService(zoneTilingJobServiceMock, eventProducerMock);

  @Test
  void fail_zone_tiling_job_on_empty_tasks() {
    var zoneTilingJobMock = mock(ZoneTilingJob.class);
    when(zoneTilingJobServiceMock.fireTasks(zoneTilingJobMock)).thenReturn(List.of());

    assertDoesNotThrow(() -> subject.accept(new ZoneTilingJobCreated(zoneTilingJobMock)));

    verify(zoneTilingJobServiceMock)
        .fail(
            zoneTilingJobMock,
            "Unable to fire tiling tasks as no tiles could be computed using provided coordinates");
    verify(eventProducerMock, never()).accept(any());
  }
}
