package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.DelimitationObjectType.PARCEL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static software.amazon.awssdk.http.HttpStatusCode.*;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.service.GeoCodeService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FeatureAddressConverterTest {
  GeoCodeService geoCodeServiceMock = mock();
  FeatureAddressConverter subject = new FeatureAddressConverter(geoCodeServiceMock);

  @Test
  void throws_exception_when_delimitation_object_type_not_building() {
    var actual =
        assertThrows(
            NotImplementedException.class, () -> subject.apply(randomUUID().toString(), PARCEL));

    assertEquals(
        "Unable to convert address to Feature for delimitationObjectType PARCEL",
        actual.getMessage());
  }

  @Test
  void return_converted_feature_when_address_is_converted_to_point_geometry() {
    var randomAddress = "address " + randomUUID();
    Feature convertedFeatureMock = mock();
    when(geoCodeServiceMock.geocode(randomAddress)).thenReturn(convertedFeatureMock);

    var actual = subject.apply(randomAddress, BUILDING);

    assertEquals(convertedFeatureMock, actual);
  }

  @Test
  void return_converted_feature_from_coordinates() {
    var randomAddress = "address " + randomUUID();
    var longitude = 1.0;
    var latitude = 2.0;
    Feature convertedFeatureMock = mock();
    when(geoCodeServiceMock.geocode(
            randomAddress, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenReturn(convertedFeatureMock);

    var actual = subject.apply(randomAddress, longitude, latitude);

    assertEquals(convertedFeatureMock, actual);
    verify(geoCodeServiceMock)
        .geocode(randomAddress, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }

  @Test
  void return_converted_feature_from_coordinates_without_address() {
    var longitude = 1.0;
    var latitude = 2.0;
    Feature convertedFeatureMock = mock();
    when(geoCodeServiceMock.geocode(
            null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenReturn(convertedFeatureMock);

    var actual = subject.apply(null, longitude, latitude);

    assertEquals(convertedFeatureMock, actual);
  }

  @Test
  void throws_exception_when_no_building_found_from_coordinates() {
    var longitude = 1.0;
    var latitude = 2.0;
    var exceptionMessage =
        "No building found for coordinates (longitude="
            + BigDecimal.valueOf(longitude)
            + ", latitude="
            + BigDecimal.valueOf(latitude)
            + ")";
    when(geoCodeServiceMock.geocode(
            null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenThrow(new BadRequestException(exceptionMessage));

    var actual =
        assertThrows(BadRequestException.class, () -> subject.apply(null, longitude, latitude));

    assertEquals(exceptionMessage, actual.getMessage());
  }
}
