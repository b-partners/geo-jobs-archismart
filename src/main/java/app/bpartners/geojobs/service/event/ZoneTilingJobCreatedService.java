package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobCreated;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneTilingJobCreatedService implements Consumer<ZoneTilingJobCreated> {
  private final ZoneTilingJobService zoneTilingJobService;
  private final EventProducer eventProducer;

  @Override
  public void accept(ZoneTilingJobCreated zoneTilingJobCreated) {
    ZoneTilingJob ztj = zoneTilingJobCreated.getZoneTilingJob();

    zoneTilingJobService.fireTasks(ztj);

    eventProducer.accept(List.of(new ZTJStatusRecomputingSubmitted(ztj.getId())));
    eventProducer.accept(List.of(new AutoTaskStatisticRecomputingSubmitted(ztj.getId())));
  }
}
