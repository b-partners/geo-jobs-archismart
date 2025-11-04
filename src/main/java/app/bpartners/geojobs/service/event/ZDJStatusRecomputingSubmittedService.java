package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.service.event.detection.ZDJStatusRecomputingSubmittedBean;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ZDJStatusRecomputingSubmittedService
    implements Consumer<ZDJStatusRecomputingSubmitted> {
  private final ZDJStatusRecomputingSubmittedBean service;

  @Override
  public void accept(ZDJStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
