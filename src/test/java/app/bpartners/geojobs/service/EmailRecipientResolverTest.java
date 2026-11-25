package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class EmailRecipientResolverTest {
  private static final String ADMIN_EMAIL = "admin@email.com";
  private static final String CLIENT_EMAIL = "client@email.com";
  private static final String COMMUNITY_ID = "communityId";
  CommunityAuthorizationRepository communityRepositoryMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  EmailRecipientResolver subject =
      new EmailRecipientResolver(communityRepositoryMock, detectionRepositoryMock, ADMIN_EMAIL);

  @Test
  void resolve_client_email_when_notification_enabled() {
    givenCommunity(true);

    assertEquals(CLIENT_EMAIL, subject.resolve(aDetection(), CLIENT_EMAIL));
  }

  @Test
  void resolve_admin_email_when_notification_disabled() {
    givenCommunity(false);

    assertEquals(ADMIN_EMAIL, subject.resolve(aDetection(), CLIENT_EMAIL));
  }

  @Test
  void notification_is_disabled_by_default() {
    assertEquals(
        false, CommunityAuthorization.builder().build().isClientEmailNotificationEnabled());
  }

  @Test
  void resolve_admin_email_when_detection_or_community_is_unknown() {
    when(communityRepositoryMock.findById(COMMUNITY_ID)).thenReturn(Optional.empty());

    assertEquals(ADMIN_EMAIL, subject.resolve((Detection) null, CLIENT_EMAIL));
    assertEquals(ADMIN_EMAIL, subject.resolve(Detection.builder().build(), CLIENT_EMAIL));
    assertEquals(ADMIN_EMAIL, subject.resolve(aDetection(), CLIENT_EMAIL));
  }

  @Test
  void resolve_by_job_id_through_tiling_job_then_detection_job() {
    givenCommunity(true);
    when(detectionRepositoryMock.findByZtjId("ztjId")).thenReturn(Optional.empty());
    when(detectionRepositoryMock.findByZdjId("zdjId")).thenReturn(Optional.of(aDetection()));

    assertEquals(CLIENT_EMAIL, subject.resolveByJobId("zdjId", CLIENT_EMAIL));
    assertEquals(ADMIN_EMAIL, subject.resolveByJobId("ztjId", CLIENT_EMAIL));
  }

  @Test
  void resolve_community_email_only_when_notification_enabled() {
    var enabled = CommunityAuthorization.builder().clientEmailNotificationEnabled(true).build();
    var disabled = CommunityAuthorization.builder().build();

    assertEquals(CLIENT_EMAIL, subject.resolve(enabled, CLIENT_EMAIL));
    assertEquals(ADMIN_EMAIL, subject.resolve(disabled, CLIENT_EMAIL));
  }

  @SneakyThrows
  @Test
  void admin_copy_is_skipped_when_admin_is_already_recipient() {
    assertEquals(List.of(), subject.adminCopyUnless(ADMIN_EMAIL));
    assertEquals(List.of(new InternetAddress(ADMIN_EMAIL)), subject.adminCopyUnless(CLIENT_EMAIL));
  }

  private void givenCommunity(boolean enabled) {
    when(communityRepositoryMock.findById(COMMUNITY_ID))
        .thenReturn(
            Optional.of(
                CommunityAuthorization.builder()
                    .id(COMMUNITY_ID)
                    .clientEmailNotificationEnabled(enabled)
                    .build()));
  }

  private static Detection aDetection() {
    return Detection.builder().communityOwnerId(COMMUNITY_ID).build();
  }
}
