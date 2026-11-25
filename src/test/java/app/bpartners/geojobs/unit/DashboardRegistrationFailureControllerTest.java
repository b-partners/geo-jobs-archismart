package app.bpartners.geojobs.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.v1.DashboardRegistrationFailureController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.DashboardRegistrationFailureRestMapper;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationFailure;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType;
import app.bpartners.geojobs.service.DashboardRegistrationFailureService;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DashboardRegistrationFailureControllerTest {
  DashboardRegistrationFailureService serviceMock = mock();
  DashboardRegistrationFailureController subject =
      new DashboardRegistrationFailureController(
          serviceMock, new DashboardRegistrationFailureRestMapper());

  @Test
  void reprocess_returns_the_failure() {
    var now = Instant.now();
    when(serviceMock.reprocess("id"))
        .thenReturn(
            DashboardRegistrationFailure.builder()
                .id("id")
                .type(DashboardRegistrationType.THREE_D_3D)
                .subjectId("request")
                .communityOwnerId("community")
                .projectKey("key")
                .payload("{}")
                .errorMessage("down")
                .attempts(2)
                .status(DashboardRegistrationStatus.PENDING)
                .creationDatetime(now)
                .lastAttemptDatetime(now)
                .build());

    var actual = subject.reprocessDashboardRegistrationFailure("id");

    assertEquals("id", actual.getId());
    assertEquals("THREE_D_3D", actual.getType());
    assertEquals("request", actual.getSubjectId());
    assertEquals("community", actual.getCommunityOwnerId());
    assertEquals("key", actual.getProjectKey());
    assertEquals(2, actual.getAttempts());
    assertEquals("PENDING", actual.getStatus());
    assertEquals(now, actual.getCreationDatetime());
  }
}
