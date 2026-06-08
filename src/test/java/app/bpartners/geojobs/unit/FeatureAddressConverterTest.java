package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.DelimitationObjectType.PARCEL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static software.amazon.awssdk.http.HttpStatusCode.*;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.FeatureAddressConverter;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.GeoPosition;
import app.bpartners.geojobs.service.dashboard.mapper.AreaPictureDetailsMapper;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.web.client.HttpClientErrorException;

class FeatureAddressConverterTest {
  AreaPictureApi areaPictureApiMock = mock();
  AreaPictureDetailsMapper areaPictureDetailsMapperMock = mock();
  String adminApiKey = randomUUID().toString();
  GeometryConverter geometryConverterMock = mock();
  BuildingFinder buildingFinderMock = mock();
  FeatureAddressConverter subject =
      new FeatureAddressConverter(
          areaPictureApiMock,
          areaPictureDetailsMapperMock,
          adminApiKey,
          geometryConverterMock,
          buildingFinderMock);

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
  void
      throws_exception_on_address_when_http_error_occurs_on_converting_address_to_point_geometry() {
    var randomAddress = "address " + randomUUID();
    CrupdateAreaPictureDetails crupdateAreaPictureDetailsMock = mock();
    when(areaPictureApiMock.crupdateAreaPictureDetails(
            anyString(), eq(crupdateAreaPictureDetailsMock), eq(adminApiKey)))
        .thenThrow(
            new HttpClientErrorException(org.springframework.http.HttpStatusCode.valueOf(403)))
        .thenThrow(
            new HttpClientErrorException(org.springframework.http.HttpStatusCode.valueOf(500)));
    when(areaPictureDetailsMapperMock.toCrupdateAreaPictureDetails(randomAddress))
        .thenReturn(crupdateAreaPictureDetailsMock);

    var actualOccurredByClientHttpException =
        assertThrows(ApiException.class, () -> subject.apply(randomAddress, BUILDING));
    var actualOccurredByHttpServerException =
        assertThrows(ApiException.class, () -> subject.apply(randomAddress, BUILDING));

    assertEquals(
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + randomAddress,
        actualOccurredByClientHttpException.getMessage());
    assertEquals(
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + randomAddress,
        actualOccurredByHttpServerException.getMessage());
  }

  @Test
  void
      throws_exception_on_address_when_any_http_errors_occurs_on_converting_address_to_point_geometry_but_longitude_is_null() {
    var randomAddress = "address " + randomUUID();
    CrupdateAreaPictureDetails crupdateAreaPictureDetailsMock = mock();
    AreaPictureDetails areaPictureDetailsMock = mock();
    GeoPosition geoPositionMock = mock();
    when(geoPositionMock.longitude()).thenReturn(null);
    when(areaPictureDetailsMock.currentGeoPosition()).thenReturn(geoPositionMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(
            anyString(), eq(crupdateAreaPictureDetailsMock), eq(adminApiKey)))
        .thenReturn(areaPictureDetailsMock);
    when(areaPictureDetailsMapperMock.toCrupdateAreaPictureDetails(randomAddress))
        .thenReturn(crupdateAreaPictureDetailsMock);

    var actual = assertThrows(ApiException.class, () -> subject.apply(randomAddress, BUILDING));

    assertEquals(
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + randomAddress,
        actual.getMessage());
  }

  @Test
  void return_converted_feature_when_address_is_converted_to_point_geometry() {
    var randomAddress = "address " + randomUUID();
    var longitudeDoubleValue = 1.0;
    var latitudeDoubleValue = 2.0;
    Feature convertedFeatureMock = mock();
    MultiPolygon nearestRoofMultiPolygonMock = mock();
    CrupdateAreaPictureDetails crupdateAreaPictureDetailsMock = mock();
    AreaPictureDetails areaPictureDetailsMock = mock();
    GeoPosition geoPositionMock = mock();
    when(geoPositionMock.longitude()).thenReturn(longitudeDoubleValue);
    when(geoPositionMock.latitude()).thenReturn(latitudeDoubleValue);
    when(areaPictureDetailsMock.currentGeoPosition()).thenReturn(geoPositionMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(
            anyString(), eq(crupdateAreaPictureDetailsMock), eq(adminApiKey)))
        .thenReturn(areaPictureDetailsMock);
    when(areaPictureDetailsMapperMock.toCrupdateAreaPictureDetails(randomAddress))
        .thenReturn(crupdateAreaPictureDetailsMock);
    when(buildingFinderMock.getBuildingMultiPolygon(
            List.of(
                BigDecimal.valueOf(longitudeDoubleValue), BigDecimal.valueOf(latitudeDoubleValue))))
        .thenReturn(nearestRoofMultiPolygonMock);
    when(geometryConverterMock.toFeature(
            anyString(), anyInt(), any(HashMap.class), any(MultiPolygon.class)))
        .thenReturn(convertedFeatureMock);

    assertDoesNotThrow(() -> subject.apply(randomAddress, BUILDING));

    HashMap<String, Object> expectedConvertedFeatureProperties = new HashMap<>();
    expectedConvertedFeatureProperties.put("address", randomAddress);
    verify(geometryConverterMock, times(1))
        .toFeature(
            eq(null),
            eq(20),
            eq(expectedConvertedFeatureProperties),
            eq(nearestRoofMultiPolygonMock));
  }
}
