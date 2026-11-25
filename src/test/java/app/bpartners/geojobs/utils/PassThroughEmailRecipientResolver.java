package app.bpartners.geojobs.utils;

import app.bpartners.geojobs.conf.EnvConf;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.EmailRecipientResolver;
import java.util.Optional;

public class PassThroughEmailRecipientResolver extends EmailRecipientResolver {
  public PassThroughEmailRecipientResolver() {
    super(null, null, EnvConf.ADMIN_EMAIL);
  }

  @Override
  public String resolve(Detection detection, String requestedEmail) {
    return detection == null ? EnvConf.ADMIN_EMAIL : requestedEmail;
  }

  @Override
  public String resolve(Optional<Detection> detection, String requestedEmail) {
    return detection.isEmpty() ? EnvConf.ADMIN_EMAIL : requestedEmail;
  }

  @Override
  public String resolveByZoneDetectionJobId(String zdjId, String requestedEmail) {
    return requestedEmail;
  }

  @Override
  public String resolveByJobId(String jobId, String requestedEmail) {
    return requestedEmail;
  }

  @Override
  public String resolve(CommunityAuthorization community, String requestedEmail) {
    return requestedEmail;
  }
}
