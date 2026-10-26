package app.bpartners.geojobs.validator;

import static app.bpartners.geojobs.validator.AddressOrPointValidator.isAddressRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class AddressOrPointValidatorTest {
  private static String messageOf(String address, Double longitude, Double latitude) {
    return assertThrows(
            BadRequestException.class, () -> isAddressRequest(address, longitude, latitude))
        .getMessage();
  }

  @Test
  void is_address_request_ok() {
    assertTrue(isAddressRequest("paris", null, null));
    assertFalse(isAddressRequest(null, 2.35, 48.85));
    assertFalse(isAddressRequest("  ", 2.35, 48.85));
  }

  @Test
  void both_address_and_coordinates_ko() {
    var expected = "Both address and point coordinates (longitude,latitude) can not be provided";

    assertEquals(expected, messageOf("paris", 2.35, 48.85));
    assertEquals(expected, messageOf("paris", 2.35, null));
    assertEquals(expected, messageOf("paris", null, 48.85));
  }

  @Test
  void neither_address_nor_coordinates_ko() {
    var expected = "Either address or point coordinates (longitude,latitude) is required";

    assertEquals(expected, messageOf(null, null, null));
    assertEquals(expected, messageOf("  ", null, null));
  }

  @Test
  void partial_coordinates_ko() {
    var expected = "Both longitude and latitude are required to geocode from point coordinates";

    assertEquals(expected, messageOf(null, 2.35, null));
    assertEquals(expected, messageOf(null, null, 48.85));
  }
}
