package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered.LANES;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.util.ArrayList;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllDashboardApiKeyCheckTriggeredService
    implements Consumer<AllDashboardApiKeyCheckTriggered> {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final EventProducer<DashboardApiKeyCheckTriggered> eventProducer;

  @Override
  public void accept(AllDashboardApiKeyCheckTriggered event) {
    var ids = communityAuthorizationRepository.findAllIdsByIntegrationTestUsageFalse();
    log.info("{} community authorizations will be checked on up to {} lanes", ids.size(), LANES);

    var firstEvents = new ArrayList<DashboardApiKeyCheckTriggered>();
    for (int i = 0; i < Math.min(LANES, ids.size()); i++) {
      int index = i;
      communityAuthorizationRepository
          .findById(ids.get(i))
          .ifPresent(a -> firstEvents.add(DashboardApiKeyCheckTriggered.of(a, index)));
    }
    if (!firstEvents.isEmpty()) {
      eventProducer.accept(firstEvents);
    }
  }
}
