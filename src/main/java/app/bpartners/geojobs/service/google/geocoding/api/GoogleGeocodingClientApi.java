package app.bpartners.geojobs.service.google.geocoding.api;

import app.bpartners.geojobs.service.google.geocoding.api.exception.GeocodingException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Client pour Google Geocoding API v4 - méthode SearchDestinations.
 *
 * <p>Cas d'usage : à partir d'une adresse OU d'un point lat/lon, récupérer l'empreinte (footprint)
 * du bâtiment au format GeoJSON standard.
 *
 * <p>Doc : https://developers.google.com/maps/documentation/geocoding/search-for-destinations
 */
@Slf4j
@Component
public class GoogleGeocodingClientApi {

  private static final String ENDPOINT = "https://geocode.googleapis.com/v4/geocode/destinations";

  /** Champs qu'on demande explicitement à l'API pour minimiser la charge utile. */
  private static final String DEFAULT_FIELD_MASK = "*";

  private final RestTemplate restTemplate;
  private final String apiKey;

  public GoogleGeocodingClientApi(
      RestTemplate restTemplate, @Value("${google.geocode.api.key}") String apiKey) {
    this.apiKey = apiKey;
    this.restTemplate = restTemplate;
  }

  // -------------- API publique --------------

  public Optional<GeoJsonFeature> findBuildingByAddress(String address) {
    var request =
        new GeocodingDtos.SearchDestinationsRequest(
            null, new GeocodingDtos.AddressQuery(address), null, "fr", "FR");
    var optionalGeoJsonFeature = search(request);
    optionalGeoJsonFeature.ifPresent(
        geoJsonFeature -> geoJsonFeature.properties().put("address", address));
    return optionalGeoJsonFeature;
  }

  public Optional<GeoJsonFeature> findBuildingByLocation(double latitude, double longitude) {
    var request =
        new GeocodingDtos.SearchDestinationsRequest(
            null,
            null,
            new GeocodingDtos.LocationQuery(
                new GeocodingDtos.LatLng(latitude, longitude),
                new GeocodingDtos.PlaceFilter("BUILDING", "PRIMARY")),
            "fr",
            "FR");
    return search(request);
  }

  // -------------- Plomberie --------------

  private Optional<GeoJsonFeature> search(GeocodingDtos.SearchDestinationsRequest body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.set("X-Goog-Api-Key", apiKey);
    headers.set("X-Goog-FieldMask", DEFAULT_FIELD_MASK);

    HttpEntity<GeocodingDtos.SearchDestinationsRequest> entity = new HttpEntity<>(body, headers);

    GeocodingDtos.SearchDestinationsResponse response;
    try {
      response =
          restTemplate
              .exchange(
                  ENDPOINT, HttpMethod.POST, entity, GeocodingDtos.SearchDestinationsResponse.class)
              .getBody();
    } catch (HttpStatusCodeException e) {
      throw new GeocodingException(
          "Geocoding API v4 a renvoyé " + e.getStatusCode() + " : " + e.getResponseBodyAsString(),
          e);
    } catch (Exception e) {
      throw new GeocodingException("Erreur d'appel à la Geocoding API v4", e);
    }

    if (response == null || response.destinations() == null || response.destinations().isEmpty()) {
      return Optional.empty();
    }

    GeocodingDtos.Destination dest = response.destinations().getFirst();
    GeocodingDtos.PlaceView primary = dest.primary();
    if (primary == null || primary.displayPolygon() == null || primary.displayPolygon().isEmpty()) {
      var containingPlaces = dest.containingPlaces();
      if (containingPlaces == null || containingPlaces.isEmpty()) {
        log.error(
            "No building found for address {} or location {}",
            body.addressQuery(),
            body.locationQuery());
        return Optional.empty();
      }
      var firstContainingPlace = containingPlaces.getFirst();
      if (firstContainingPlace == null
          || firstContainingPlace.displayPolygon() == null
          || firstContainingPlace.displayPolygon().isEmpty()) {
        return Optional.empty();
      }
      return getGeoJsonFeature(firstContainingPlace);
    }

    // displayPolygon est déjà du GeoJSON RFC 7946 (Polygon ou MultiPolygon).
    // On l'embarque tel quel dans une Feature, en ajoutant nos métadonnées.
    return getGeoJsonFeature(primary);
  }

  @NotNull
  private Optional<GeoJsonFeature> getGeoJsonFeature(GeocodingDtos.PlaceView placeView) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("placeId", placeView.place());
    if (placeView.displayName() != null) {
      properties.put("displayName", placeView.displayName().text());
    }
    properties.put("formattedAddress", placeView.formattedAddress());
    properties.put("structureType", placeView.structureType());
    properties.put("source", "google-geocoding-v4");
    return Optional.of(GeoJsonFeature.of(placeView.displayPolygon(), properties));
  }
}
