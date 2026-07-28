package app.bpartners.geojobs.service.geojobs;

import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoCodingJobCreated;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.GeoCodingJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.GeoCodeService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.google.maps.GeoPosition;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.ArgumentCaptor;

class GeoCodeServiceTest {
  GeoCodeApi geoCodeApiMock = mock();
  GeometryConverter geometryConverterMock = mock();
  GeoCodingJobRepository geoCodingJobRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  EventProducer eventProducerMock = mock();
  BuildingFinder buildingFinderMock = mock();

  GeoCodeService subject =
      new GeoCodeService(
          geoCodeApiMock,
          geometryConverterMock,
          geoCodingJobRepositoryMock,
          bucketComponentMock,
          eventProducerMock,
          buildingFinderMock);

  @SneakyThrows
  @Test
  void submit_geo_coding_job_through_excel_keeps_provided_sheet_index_ok() {
    var endToEndId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var sheetIndex = 3;
    var excelFile = File.createTempFile("addresses-", ".xlsx");
    when(geoCodingJobRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            endToEndId, communityOwnerId))
        .thenReturn(Optional.empty());
    when(geoCodingJobRepositoryMock.save(any(GeoCodingJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual =
        subject.submitGeoCodingJobThroughExcel(endToEndId, communityOwnerId, excelFile, sheetIndex);

    assertEquals(sheetIndex, actual.getSheetIndex());
    assertEquals(endToEndId, actual.getEndToEndId());
    assertEquals(communityOwnerId, actual.getCommunityOwnerId());
    assertEquals(PROCESSING, actual.getStatus());
    verify(bucketComponentMock).upload(excelFile, "geocoding/" + actual.getId() + ".xlsx");
    verify(eventProducerMock).accept(List.of(new GeoCodingJobCreated(actual.getId())));
    var savedJobCaptor = ArgumentCaptor.forClass(GeoCodingJob.class);
    verify(geoCodingJobRepositoryMock).save(savedJobCaptor.capture());
    assertEquals(sheetIndex, savedJobCaptor.getValue().getSheetIndex());
  }

  @SneakyThrows
  @Test
  void submit_geo_coding_job_through_excel_without_sheet_index_ok() {
    var endToEndId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var excelFile = File.createTempFile("addresses-", ".xlsx");
    when(geoCodingJobRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            endToEndId, communityOwnerId))
        .thenReturn(Optional.empty());
    when(geoCodingJobRepositoryMock.save(any(GeoCodingJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual =
        subject.submitGeoCodingJobThroughExcel(endToEndId, communityOwnerId, excelFile, null);

    assertNull(actual.getSheetIndex());
  }

  @SneakyThrows
  @Test
  void submit_geo_coding_job_through_excel_of_first_sheet_index_ok() {
    var endToEndId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var excelFile = File.createTempFile("addresses-", ".xlsx");
    when(geoCodingJobRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            endToEndId, communityOwnerId))
        .thenReturn(Optional.empty());
    when(geoCodingJobRepositoryMock.save(any(GeoCodingJob.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual = subject.submitGeoCodingJobThroughExcel(endToEndId, communityOwnerId, excelFile, 1);

    assertEquals(1, actual.getSheetIndex());
    assertEquals(PROCESSING, actual.getStatus());
  }

  @SneakyThrows
  @Test
  void submit_geo_coding_job_through_excel_of_sheet_index_lower_than_one_ko() {
    var endToEndId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var excelFile = File.createTempFile("addresses-", ".xlsx");

    var actualWithZero =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.submitGeoCodingJobThroughExcel(endToEndId, communityOwnerId, excelFile, 0));
    var actualWithNegative =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.submitGeoCodingJobThroughExcel(
                    endToEndId, communityOwnerId, excelFile, -3));

    assertEquals(
        "Sheet index must be greater than or equal to 1. Actual value: 0",
        actualWithZero.getMessage());
    assertEquals(
        "Sheet index must be greater than or equal to 1. Actual value: -3",
        actualWithNegative.getMessage());
    verifyNoInteractions(geoCodingJobRepositoryMock);
    verifyNoInteractions(bucketComponentMock);
    verifyNoInteractions(eventProducerMock);
  }

  @SneakyThrows
  @Test
  void submit_geo_coding_job_through_excel_of_already_processed_job_ko() {
    var endToEndId = randomUUID().toString();
    var communityOwnerId = randomUUID().toString();
    var excelFile = File.createTempFile("addresses-", ".xlsx");
    when(geoCodingJobRepositoryMock.findByEndToEndIdAndCommunityOwnerId(
            endToEndId, communityOwnerId))
        .thenReturn(Optional.of(GeoCodingJob.builder().id(randomUUID().toString()).build()));

    var actual =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.submitGeoCodingJobThroughExcel(endToEndId, communityOwnerId, excelFile, 2));

    assertEquals(
        "Processed GeoCodingJob(id=" + endToEndId + ") can not be updated", actual.getMessage());
    verifyNoInteractions(bucketComponentMock);
    verifyNoInteractions(eventProducerMock);
  }

  @Test
  void geocode_address_ok() {
    var address = "random-address-" + randomUUID();

    var mockFeature = mock(Feature.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(buildingFinderMock.getBuildingMultiPolygon(address)).thenReturn(multiPolygonMock);
    when(geometryConverterMock.toFeature(eq(null), eq(20), any(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address);

    assertEquals(mockFeature, actual);
  }

  @SneakyThrows
  @Test
  void geocode_address_through_point_ok() {
    var address = "random-address-" + randomUUID();
    var latitude = Math.random();
    var longitude = Math.random();

    var mockFeature = mock(Feature.class);
    var geoPositionMock = mock(GeoPosition.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(geoPositionMock.latitude()).thenReturn(latitude);
    when(geoPositionMock.longitude()).thenReturn(longitude);
    when(geoCodeApiMock.searchGeoPositionFromAddress(address)).thenReturn(geoPositionMock);
    when(buildingFinderMock.getBuildingMultiPolygon(address))
        .thenThrow(new RuntimeException("Unexpected error"));
    when(buildingFinderMock.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude))))
        .thenReturn(multiPolygonMock);
    when(geometryConverterMock.toFeature(eq(null), eq(20), any(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address);

    assertEquals(mockFeature, actual);
  }

  @SneakyThrows
  @Test
  void geocode_address_without_building_falls_back_on_geo_code_api_ok() {
    var address = "random-address-" + randomUUID();
    var latitude = Math.random();
    var longitude = Math.random();

    var mockFeature = mock(Feature.class);
    var geoPositionMock = mock(GeoPosition.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(geoPositionMock.latitude()).thenReturn(latitude);
    when(geoPositionMock.longitude()).thenReturn(longitude);
    when(geoCodeApiMock.searchGeoPositionFromAddress(address)).thenReturn(geoPositionMock);
    when(buildingFinderMock.getBuildingMultiPolygon(address)).thenReturn(null);
    when(buildingFinderMock.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude))))
        .thenReturn(multiPolygonMock);
    when(geometryConverterMock.toFeature(eq(null), eq(20), any(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address);

    assertEquals(mockFeature, actual);
  }

  @SneakyThrows
  @Test
  void geocode_address_without_building_anywhere_ko() {
    var address = "random-address-" + randomUUID();
    var latitude = 48.85;
    var longitude = 2.35;

    var geoPositionMock = mock(GeoPosition.class);
    when(geoPositionMock.latitude()).thenReturn(latitude);
    when(geoPositionMock.longitude()).thenReturn(longitude);
    when(geoCodeApiMock.searchGeoPositionFromAddress(address)).thenReturn(geoPositionMock);
    when(buildingFinderMock.getBuildingMultiPolygon(address)).thenReturn(null);
    when(buildingFinderMock.getBuildingMultiPolygon(
            List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude))))
        .thenReturn(null);

    var actual = assertThrows(BadRequestException.class, () -> subject.geocode(address));

    assertEquals(
        "No building found for address : "
            + address
            + " at coordinates (longitude="
            + BigDecimal.valueOf(longitude)
            + ", latitude="
            + BigDecimal.valueOf(latitude)
            + ")",
        actual.getMessage());
    verifyNoInteractions(geometryConverterMock);
  }

  @Test
  void geocode_point_without_building_ko() {
    var latitude = BigDecimal.valueOf(48.85);
    var longitude = BigDecimal.valueOf(2.35);
    when(buildingFinderMock.getBuildingMultiPolygon(List.of(longitude, latitude))).thenReturn(null);

    var actual =
        assertThrows(BadRequestException.class, () -> subject.geocode(null, longitude, latitude));

    assertEquals(
        "No building found for coordinates (longitude="
            + longitude
            + ", latitude="
            + latitude
            + ")",
        actual.getMessage());
    verifyNoInteractions(geometryConverterMock);
  }

  @Test
  void geocode_point_without_address_ok() {
    var latitude = BigDecimal.valueOf(48.85);
    var longitude = BigDecimal.valueOf(2.35);

    var mockFeature = mock(Feature.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(buildingFinderMock.getBuildingMultiPolygon(List.of(longitude, latitude)))
        .thenReturn(multiPolygonMock);
    var propertiesCaptor = ArgumentCaptor.forClass(Map.class);
    when(geometryConverterMock.toFeature(
            eq(null), eq(20), propertiesCaptor.capture(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(null, longitude, latitude);

    assertEquals(mockFeature, actual);
    assertFalse(propertiesCaptor.getValue().containsKey("address"));
  }

  @Test
  void geocode_point_with_address_keeps_address_property_ok() {
    var address = "random-address-" + randomUUID();
    var latitude = BigDecimal.valueOf(48.85);
    var longitude = BigDecimal.valueOf(2.35);

    var mockFeature = mock(Feature.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    when(buildingFinderMock.getBuildingMultiPolygon(List.of(longitude, latitude)))
        .thenReturn(multiPolygonMock);
    var propertiesCaptor = ArgumentCaptor.forClass(Map.class);
    when(geometryConverterMock.toFeature(
            eq(null), eq(20), propertiesCaptor.capture(), eq(multiPolygonMock)))
        .thenReturn(mockFeature);

    var actual = subject.geocode(address, longitude, latitude);

    assertEquals(mockFeature, actual);
    assertEquals(address, propertiesCaptor.getValue().get("address"));
  }

  @Test
  void geocode_address_missing_ko() {
    var actual = assertThrows(BadRequestException.class, () -> subject.geocode(null));
    var actualBlank = assertThrows(BadRequestException.class, () -> subject.geocode(""));

    assertEquals("Address is mandatory", actual.getMessage());
    assertEquals("Address is mandatory", actualBlank.getMessage());
  }

  @SneakyThrows
  @Test
  void geocode_address_ko() {
    var address = "random-address-" + randomUUID();
    when(buildingFinderMock.getBuildingMultiPolygon(address)).thenThrow(new RuntimeException());
    when(geoCodeApiMock.searchGeoPositionFromAddress(address)).thenThrow(new IOException());

    var actual = assertThrows(BadRequestException.class, () -> subject.geocode(address));

    assertEquals("Unable to geocode address : " + address, actual.getMessage());
  }
}
