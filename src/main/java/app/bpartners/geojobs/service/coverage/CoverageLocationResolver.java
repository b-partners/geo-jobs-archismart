package app.bpartners.geojobs.service.coverage;

import static app.bpartners.geojobs.validator.AddressOrPointValidator.isAddressRequest;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoverageLocationResolver {
  private static final double MAX_LONGITUDE = 180;
  private static final double MAX_LATITUDE = 90;

  private final GeoCodeApi geoCodeApi;

  public CoverageLocation resolve(String address, Double longitude, Double latitude) {
    return isAddressRequest(address, longitude, latitude)
        ? fromAddress(address)
        : fromCoordinates(longitude, latitude);
  }

  private CoverageLocation fromCoordinates(Double longitude, Double latitude) {
    if (Math.abs(longitude) > MAX_LONGITUDE || Math.abs(latitude) > MAX_LATITUDE) {
      throw new BadRequestException(
          "Coordinates out of range : longitude must be within [-180,180] and latitude within"
              + " [-90,90]");
    }
    return new CoverageLocation(longitude, latitude);
  }

  private CoverageLocation fromAddress(String address) {
    try {
      var position = geoCodeApi.searchGeoPositionFromAddress(address);
      return new CoverageLocation(position.longitude(), position.latitude());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw unableToGeocode(address);
    } catch (IOException | ApiException | RuntimeException e) {
      throw unableToGeocode(address);
    }
  }

  private static BadRequestException unableToGeocode(String address) {
    return new BadRequestException("Unable to geocode address : " + address);
  }
}
