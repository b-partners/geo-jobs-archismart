package app.bpartners.geojobs.service.google.geocoding.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

public final class GeocodingDtos {

  private GeocodingDtos() {}

  // ---------- Requête ----------

  /**
   * primary_query est un union : on remplit EXACTEMENT UN parmi place / addressQuery /
   * locationQuery.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SearchDestinationsRequest(
      String place,
      AddressQuery addressQuery,
      LocationQuery locationQuery,
      String languageCode,
      String regionCode) {}

  /**
   * AddressQuery est lui-même un union : on utilise le champ "addressQuery" pour passer l'adresse
   * sur une seule ligne (string).
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record AddressQuery(String addressQuery) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LocationQuery(LatLng location, PlaceFilter placeFilter) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record PlaceFilter(
      String structureType, // "BUILDING", "GROUNDS", ...
      String addressability // "PRIMARY", "WEAK", "ANY"
      ) {}

  public record LatLng(double latitude, double longitude) {}

  // ---------- Réponse ----------

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SearchDestinationsResponse(List<Destination> destinations) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Destination(
      PlaceView primary,
      List<PlaceView> containingPlaces,
      List<PlaceView> subDestinations,
      List<Entrance> entrances) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlaceView(
      String place,
      DisplayName displayName,
      String formattedAddress,
      String structureType,
      LatLng location,
      /*
       * displayPolygon est une géométrie GeoJSON RFC 7946 (Polygon ou MultiPolygon).
       * On la garde en Map<String,Object> pour la passer telle quelle dans notre Feature.
       * Coordonnées déjà en [longitude, latitude].
       */
      Map<String, Object> displayPolygon) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DisplayName(String text, String languageCode) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Entrance(LatLng location, DisplayName displayName, List<String> tags) {}
}
