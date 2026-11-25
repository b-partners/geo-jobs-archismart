package app.bpartners.geojobs.repository.model.dashboard;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "dashboard_registration_failure")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DashboardRegistrationFailure implements Serializable {
  @Id private String id;

  @Enumerated(STRING)
  private DashboardRegistrationType type;

  @Column(name = "subject_id")
  private String subjectId;

  @Column(name = "community_owner_id")
  private String communityOwnerId;

  @Column(name = "project_key")
  private String projectKey;

  private String payload;

  @Column(name = "error_message")
  private String errorMessage;

  private int attempts;

  @Enumerated(STRING)
  private DashboardRegistrationStatus status;

  @Column(name = "creation_datetime")
  private Instant creationDatetime;

  @Column(name = "last_attempt_datetime")
  private Instant lastAttemptDatetime;

  @Column(name = "reprocessed_datetime")
  private Instant reprocessedDatetime;
}
