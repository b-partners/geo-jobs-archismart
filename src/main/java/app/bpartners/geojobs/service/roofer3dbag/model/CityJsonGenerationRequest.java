package app.bpartners.geojobs.service.roofer3dbag.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Corps de requête pour POST /cityjson/generate. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityJsonGenerationRequest {

  /** Polygone d'emprise du bâtiment en EPSG:2154. Exemple : s3://bucket/emprise.geojson */
  @JsonProperty("geoJsonBuildingPresignedUrl")
  private String geoJsonBuildingPresignedUrl;

  /** Liste des fichiers LiDAR (.copc.laz) sur S3. */
  @JsonProperty("lidarPresignedUrls")
  private List<String> lidarPresignedUrls;
}
