package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailRecipientResolver {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectionRepository detectionRepository;
  private final String adminEmail;

  public EmailRecipientResolver(
      CommunityAuthorizationRepository communityAuthorizationRepository,
      DetectionRepository detectionRepository,
      @Value("${admin.email}") String adminEmail) {
    this.communityAuthorizationRepository = communityAuthorizationRepository;
    this.detectionRepository = detectionRepository;
    this.adminEmail = adminEmail;
  }

  public List<InternetAddress> adminCopyUnless(String to) throws AddressException {
    return adminEmail.equalsIgnoreCase(to) ? List.of() : List.of(new InternetAddress(adminEmail));
  }

  public String resolve(Detection detection, String requestedEmail) {
    if (detection == null) {
      return adminEmail;
    }
    return resolve(detection.getCommunityOwnerId(), requestedEmail);
  }

  public String resolve(Optional<Detection> detection, String requestedEmail) {
    return resolve(detection.orElse(null), requestedEmail);
  }

  public String resolveByZoneDetectionJobId(String zdjId, String requestedEmail) {
    if (zdjId == null) {
      return adminEmail;
    }
    return resolve(detectionRepository.findByZdjId(zdjId), requestedEmail);
  }

  public String resolveByJobId(String jobId, String requestedEmail) {
    var detection =
        detectionRepository.findByZtjId(jobId).or(() -> detectionRepository.findByZdjId(jobId));
    return resolve(detection, requestedEmail);
  }

  public String resolve(CommunityAuthorization community, String requestedEmail) {
    return community != null && community.isClientEmailNotificationEnabled()
        ? requestedEmail
        : adminEmail;
  }

  private String resolve(String communityOwnerId, String requestedEmail) {
    if (communityOwnerId == null) {
      return adminEmail;
    }
    return resolve(
        communityAuthorizationRepository.findById(communityOwnerId).orElse(null), requestedEmail);
  }
}
