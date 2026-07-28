package app.bpartners.geojobs.repository.model.geocoding;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;

@Slf4j
@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "geo_coding_job")
public class GeoCodingJob {
  @Id private String id;

  private String endToEndId;

  private String communityOwnerId;

  private String fileKey;

  private String geoJsonKey;

  private Integer sheetIndex;

  private String message;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private GeoCodingJobStatus status;

  @Column(nullable = false, updatable = false)
  private Instant creationDatetime;

  @PrePersist
  protected void onCreate() {
    this.creationDatetime = Instant.now();
  }
}
