package app.bpartners.geojobs.service.google.maps;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GeoCodeApi {
  private final GeoApiContext geoApiContext;
  private final AddressValidator addressValidator;

  public GeoCodeApi(
      @Value("${google.geocode.api.key}") String apiKey, AddressValidator addressValidator) {
    this.geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
    this.addressValidator = addressValidator;
  }

  public GeoPosition searchGeoPositionFromAddress(String address)
      throws IOException, InterruptedException, ApiException {
    addressValidator.accept(address);
    GeocodingResult[] geocodingResults = GeocodingApi.geocode(this.geoApiContext, address).await();
    if (geocodingResults.length == 0) {
      throw new IllegalStateException("No geocoding result for address " + address);
    }
    GeocodingResult response = geocodingResults[0];
    LatLng location = response.geometry.location;
    GeoPosition position = new GeoPosition(location.lat, location.lng);
    log.info("GeoCode Position={}", position);
    return position;
  }
}
