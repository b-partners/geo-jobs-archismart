package app.bpartners.geojobs.repository.model;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.repository.model.tiling.Tile;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Parcel implements Serializable {
  @Id private String id;

  @JdbcTypeCode(JSON)
  private ParcelContent parcelContent;

  public Parcel duplicate(String parcelId, String parcelContentId, boolean hasSameTile) {
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(parcelContent.duplicate(parcelContentId, hasSameTile))
        .build();
  }

  public Parcel duplicate(String parcelId, String parcelContentId, List<Tile> tiles) {
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(parcelContent.duplicate(parcelContentId, tiles))
        .build();
  }
}
