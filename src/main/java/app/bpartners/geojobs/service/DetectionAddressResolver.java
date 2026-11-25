package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionAddressResolver implements Function<Detection, String> {
  private static final String FORMATTED_ADDRESS_PROPERTY = "formattedAddress";
  private static final Pattern LATITUDE_LONGITUDE_PATTERN =
      Pattern.compile("^\\s*([-+]?\\d{1,3}(?:\\.\\d+)?)\\s*,\\s*([-+]?\\d{1,3}(?:\\.\\d+)?)\\s*$");
  private final GoogleGeocodingClientApi geocodingClientApi;

  @Override
  public String apply(Detection detection) {
    var zoneName = detection.getZoneName();
    var coordinates = parseLatitudeLongitude(zoneName);
    if (coordinates == null) {
      return zoneName;
    }
    try {
      return geocodingClientApi
          .findBuildingByLocation(coordinates[0], coordinates[1])
          .map(building -> building.properties().get(FORMATTED_ADDRESS_PROPERTY))
          .filter(address -> address instanceof String value && !value.isBlank())
          .map(Object::toString)
          .orElse(zoneName);
    } catch (RuntimeException e) {
      log.warn("Unable to reverse geocode zoneName {} : {}", zoneName, e.getMessage());
      return zoneName;
    }
  }

  private static double[] parseLatitudeLongitude(String zoneName) {
    if (zoneName == null) {
      return null;
    }
    var matcher = LATITUDE_LONGITUDE_PATTERN.matcher(zoneName);
    if (!matcher.matches()) {
      return null;
    }
    var latitude = Double.parseDouble(matcher.group(1));
    var longitude = Double.parseDouble(matcher.group(2));
    if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
      return null;
    }
    return new double[] {latitude, longitude};
  }
}
