package app.bpartners.geojobs.service.roofer3dbag.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse de POST /cityjson/generate. Contient l'URL pré-signée vers le CityJSON généré et sa date
 * d'expiration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityJsonGenerationResponse {

  @JsonProperty("cityJsonUrl")
  private String cityJsonUrl;

  @JsonProperty("expirationDateTime")
  private Instant expirationDateTime;
}
