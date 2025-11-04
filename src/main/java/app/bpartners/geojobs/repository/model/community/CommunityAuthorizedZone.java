package app.bpartners.geojobs.repository.model.community;

import static org.hibernate.type.SqlTypes.JSON;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "community_authorized_zone")
public class CommunityAuthorizedZone implements Serializable {
  @Id private String id;

  @Column private String name;

  @JdbcTypeCode(JSON)
  private MultiPolygon multiPolygon;

  @JoinColumn(referencedColumnName = "id")
  private String communityAuthorizationId;
}
