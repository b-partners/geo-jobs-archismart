package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.DelimitationObjectType.PARCEL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static software.amazon.awssdk.http.HttpStatusCode.*;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.service.GeoCodeService;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.GeoPosition;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;

class FeatureAddressConverterTest {
  GeometryConverter geometryConverterMock = mock();
  BuildingFinder buildingFinderMock = mock();
  GeoCodeService geoCodeServiceMock = mock();
  FeatureAddressConverter subject =
      new FeatureAddressConverter(geometryConverterMock, buildingFinderMock, geoCodeServiceMock);

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
    var longitudeDoubleValue = 1.0;
    var latitudeDoubleValue = 2.0;
    Feature convertedFeatureMock = mock();
    MultiPolygon nearestRoofMultiPolygonMock = mock();
    AreaPictureDetails areaPictureDetailsMock = mock();
    GeoPosition geoPositionMock = mock();
    when(geoPositionMock.longitude()).thenReturn(longitudeDoubleValue);
    when(geoPositionMock.latitude()).thenReturn(latitudeDoubleValue);
    when(areaPictureDetailsMock.currentGeoPosition()).thenReturn(geoPositionMock);
    when(geoCodeServiceMock.geocode(randomAddress)).thenReturn(convertedFeatureMock);
    when(buildingFinderMock.getBuildingMultiPolygon(
            List.of(
                BigDecimal.valueOf(longitudeDoubleValue), BigDecimal.valueOf(latitudeDoubleValue))))
        .thenReturn(nearestRoofMultiPolygonMock);
    when(geometryConverterMock.toFeature(
            anyString(), anyInt(), any(HashMap.class), any(MultiPolygon.class)))
        .thenReturn(convertedFeatureMock);

    assertDoesNotThrow(() -> subject.apply(randomAddress, BUILDING));
  }
}
