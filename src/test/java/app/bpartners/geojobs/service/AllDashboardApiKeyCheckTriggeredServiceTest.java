package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered.LANES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AllDashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.event.AllDashboardApiKeyCheckTriggeredService;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AllDashboardApiKeyCheckTriggeredServiceTest {
  CommunityAuthorizationRepository repository = mock();
  EventProducer<DashboardApiKeyCheckTriggered> eventProducer = mock();
  AllDashboardApiKeyCheckTriggeredService subject =
      new AllDashboardApiKeyCheckTriggeredService(repository, eventProducer);

  @Test
  void accept_starts_one_lane_per_authorization_up_to_lanes() {
    var ids = IntStream.range(0, LANES + 5).mapToObj(i -> "id-" + i).toList();
    when(repository.findAllIdsByIntegrationTestUsageFalse()).thenReturn(ids);
    ids.forEach(
        id -> {
          var authorization = authorization(id);
          when(repository.findById(id)).thenReturn(Optional.of(authorization));
        });
    var expected =
        IntStream.range(0, LANES)
            .mapToObj(i -> DashboardApiKeyCheckTriggered.of(authorization("id-" + i), i))
            .toList();

    subject.accept(new AllDashboardApiKeyCheckTriggered());

    verify(eventProducer).accept(expected);
  }

  @Test
  void accept_with_fewer_authorizations_than_lanes_starts_fewer_lanes() {
    when(repository.findAllIdsByIntegrationTestUsageFalse()).thenReturn(List.of("id-0"));
    var authorization = authorization("id-0");
    when(repository.findById("id-0")).thenReturn(Optional.of(authorization));
    var expected = List.of(DashboardApiKeyCheckTriggered.of(authorization, 0));

    subject.accept(new AllDashboardApiKeyCheckTriggered());

    verify(eventProducer).accept(expected);
  }

  @Test
  void accept_without_authorization_sends_nothing() {
    when(repository.findAllIdsByIntegrationTestUsageFalse()).thenReturn(List.of());

    subject.accept(new AllDashboardApiKeyCheckTriggered());

    verifyNoInteractions(eventProducer);
  }

  static CommunityAuthorization authorization(String id) {
    var authorization = mock(CommunityAuthorization.class);
    when(authorization.getId()).thenReturn(id);
    when(authorization.getEmail()).thenReturn(id + "@test.com");
    when(authorization.getDashboardApiKey()).thenReturn("key-" + id);
    return authorization;
  }
}
