// src/main/java/com/example/geocoding/geojson/GeoJsonFeature.java
package app.bpartners.geojobs.service.google.geocoding.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Représentation GeoJSON standard (RFC 7946). On ne modélise pas la géométrie : on accepte ce que
 * Google nous renvoie (déjà du GeoJSON conforme — Polygon ou MultiPolygon).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoJsonFeature(
    String type, Map<String, Object> geometry, Map<String, Object> properties) {
  public static GeoJsonFeature of(Map<String, Object> geometry, Map<String, Object> properties) {
    return new GeoJsonFeature("Feature", geometry, properties);
  }
}
