package app.bpartners.geojobs.repository.model.detection;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "detectable_object_type")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class DetectableObjectType implements Serializable {
  @Id private String id;

  @JoinColumn(referencedColumnName = "id", table = "\"detected_object\"")
  private String objectId;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private DetectableType detectableType;
}
