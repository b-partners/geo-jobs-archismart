package app.bpartners.geojobs.repository.model.detection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "model_card_version")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ModelCardVersion implements Serializable {
  @Id private String id;

  @Column(name = "model_card_id")
  private String modelCardId;

  private int version;

  @Column(name = "creation_datetime")
  private Instant creationDatetime;
}
