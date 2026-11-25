package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import app.bpartners.geojobs.endpoint.rest.model.DashboardRegistrationFailure;
import org.springframework.stereotype.Component;

@Component
public class DashboardRegistrationFailureRestMapper {

  public DashboardRegistrationFailure toRest(
      app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationFailure domain) {
    return new DashboardRegistrationFailure()
        .id(domain.getId())
        .type(domain.getType().name())
        .subjectId(domain.getSubjectId())
        .communityOwnerId(domain.getCommunityOwnerId())
        .projectKey(domain.getProjectKey())
        .payload(domain.getPayload())
        .errorMessage(domain.getErrorMessage())
        .attempts(domain.getAttempts())
        .status(domain.getStatus().name())
        .creationDatetime(domain.getCreationDatetime())
        .lastAttemptDatetime(domain.getLastAttemptDatetime())
        .reprocessedDatetime(domain.getReprocessedDatetime());
  }
}
