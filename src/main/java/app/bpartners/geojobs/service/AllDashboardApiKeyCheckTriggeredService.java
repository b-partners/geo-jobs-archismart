package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AllDashboardApiKeyCheckTriggeredService
    implements Consumer<AllDashboardApiKeyCheckTriggered> {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllDashboardApiKeyCheckTriggered event) {
    List<CommunityAuthorization> allCommunityAuthorizations =
        communityAuthorizationRepository.findAll();

    eventProducer.accept(allCommunityAuthorizations.stream().map(this::toTypedEvent).toList());
  }

  private DashboardApiKeyCheckTriggered toTypedEvent(CommunityAuthorization authorization) {
    return DashboardApiKeyCheckTriggered.builder().communityAuthorization(authorization).build();
  }
}
