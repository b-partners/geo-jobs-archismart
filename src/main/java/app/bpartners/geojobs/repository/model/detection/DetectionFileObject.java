package app.bpartners.geojobs.repository.model.detection;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder(toBuilder = true)
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "detection_file_object")
public class DetectionFileObject {
  @Id private String id;

  @Column(name = "id_detection")
  private String detectionIdentifier;

  private String bucketKey;

  private String fileName;

  @JdbcTypeCode(NAMED_ENUM)
  private DetectionFileType fileType;

  private Instant creationDatetime;
}
