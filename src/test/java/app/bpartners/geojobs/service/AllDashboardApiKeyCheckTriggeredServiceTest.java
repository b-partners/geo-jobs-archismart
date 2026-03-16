package app.bpartners.geojobs.service;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllDashboardApiKeyCheckTriggeredServiceTest {
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock =
      mock(CommunityAuthorizationRepository.class);
  EventProducer eventProducerMock = mock(EventProducer.class);

  AllDashboardApiKeyCheckTriggeredService subject;

  @Test
  void all_dashboard_key_check_trigger_dashboard_key_check() {
    List<CommunityAuthorization> communityAuthorizations =
        multipleCommunityAuthorization(mock(CommunityAuthorization.class));
    List<DashboardApiKeyCheckTriggered> childEvents = toChildEvents(communityAuthorizations);

    when(communityAuthorizationRepositoryMock.findAll()).thenReturn(communityAuthorizations);
    doNothing().when(eventProducerMock).accept(eq(childEvents));
    subject =
        new AllDashboardApiKeyCheckTriggeredService(
            communityAuthorizationRepositoryMock, eventProducerMock);

    subject.accept(new AllDashboardApiKeyCheckTriggered());

    verify(communityAuthorizationRepositoryMock, times(1)).findAll();
    verify(eventProducerMock, times(1)).accept(eq(childEvents));
  }

  private List<CommunityAuthorization> multipleCommunityAuthorization(
      CommunityAuthorization communityAuthorization) {
    return List.of(communityAuthorization, communityAuthorization, communityAuthorization);
  }

  private List<DashboardApiKeyCheckTriggered> toChildEvents(
      List<CommunityAuthorization> communityAuthorizations) {
    return communityAuthorizations.stream().map(DashboardApiKeyCheckTriggered::new).toList();
  }
}
