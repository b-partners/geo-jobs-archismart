package app.bpartners.geojobs.validator;

import app.bpartners.geojobs.model.exception.BadRequestException;

public final class AddressOrPointValidator {
  private AddressOrPointValidator() {}

  public static boolean isAddressRequest(String address, Double longitude, Double latitude) {
    var hasAddress = address != null && !address.isBlank();
    var hasCoordinates = longitude != null || latitude != null;
    if (hasAddress && hasCoordinates) {
      throw new BadRequestException(
          "Both address and point coordinates (longitude,latitude) can not be provided");
    }
    if (!hasAddress && !hasCoordinates) {
      throw new BadRequestException(
          "Either address or point coordinates (longitude,latitude) is required");
    }
    if (!hasAddress && (longitude == null || latitude == null)) {
      throw new BadRequestException(
          "Both longitude and latitude are required to geocode from point coordinates");
    }
    return hasAddress;
  }
}
