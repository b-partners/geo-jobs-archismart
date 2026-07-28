package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import app.bpartners.geojobs.endpoint.rest.controller.v1.GeoCodeController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.GeoCodingJobRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob;
import app.bpartners.geojobs.service.GeoCodeService;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

class GeoCodeControllerTest {
  GeoCodeService serviceMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  AuthProvider authProviderMock = mock();
  GeoCodingJobRestMapper geoCodingJobRestMapperMock = mock();

  GeoCodeController subject =
      new GeoCodeController(
          serviceMock,
          communityAuthorizationRepositoryMock,
          authProviderMock,
          geoCodingJobRestMapperMock);

  @SneakyThrows
  private static Feature domainFeature() {
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(new BigDecimal("2.35"), new BigDecimal("48.85")),
                    List.of(new BigDecimal("2.36"), new BigDecimal("48.85")),
                    List.of(new BigDecimal("2.36"), new BigDecimal("48.86")),
                    List.of(new BigDecimal("2.35"), new BigDecimal("48.85")))));
    return Feature.builder()
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(
                    objectMapper().writeValueAsString(new MultiPolygon().coordinates(coordinates)))
                .build())
        .build();
  }

  @Test
  void get_geocode_by_address_ok() {
    var address = "1 rue de la vau saint jacques parthenay";
    var domainFeature = domainFeature();
    when(serviceMock.geocode(address)).thenReturn(domainFeature);

    var actual = subject.getGeocode(address, null, null);

    assertEquals(toRestFeature(domainFeature), actual);
    verify(serviceMock).geocode(address);
  }

  @Test
  void get_geocode_by_point_coordinates_ok() {
    var longitude = 2.35;
    var latitude = 48.85;
    var domainFeature = domainFeature();
    when(serviceMock.geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenReturn(domainFeature);

    var actual = subject.getGeocode(null, latitude, longitude);

    assertEquals(toRestFeature(domainFeature), actual);
    verify(serviceMock).geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }

  @Test
  void get_geocode_by_point_coordinates_with_blank_address_ok() {
    var longitude = 2.35;
    var latitude = 48.85;
    var domainFeature = domainFeature();
    when(serviceMock.geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)))
        .thenReturn(domainFeature);

    var actual = subject.getGeocode("  ", latitude, longitude);

    assertEquals(toRestFeature(domainFeature), actual);
    verify(serviceMock).geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude));
  }

  @Test
  void get_geocode_with_both_address_and_coordinates_ko() {
    var actualWithBoth =
        assertThrows(
            BadRequestException.class, () -> subject.getGeocode("some address", 48.85, 2.35));
    var actualWithLatitudeOnly =
        assertThrows(
            BadRequestException.class, () -> subject.getGeocode("some address", 48.85, null));
    var actualWithLongitudeOnly =
        assertThrows(
            BadRequestException.class, () -> subject.getGeocode("some address", null, 2.35));

    var expectedMessage =
        "Both address and point coordinates (longitude,latitude) can not be provided";
    assertEquals(expectedMessage, actualWithBoth.getMessage());
    assertEquals(expectedMessage, actualWithLatitudeOnly.getMessage());
    assertEquals(expectedMessage, actualWithLongitudeOnly.getMessage());
    verifyNoInteractions(serviceMock);
  }

  @Test
  void get_geocode_with_partial_coordinates_ko() {
    var actualWithLatitudeOnly =
        assertThrows(BadRequestException.class, () -> subject.getGeocode(null, 48.85, null));
    var actualWithLongitudeOnly =
        assertThrows(BadRequestException.class, () -> subject.getGeocode(null, null, 2.35));

    var expectedMessage =
        "Both longitude and latitude are required to geocode from point coordinates";
    assertEquals(expectedMessage, actualWithLatitudeOnly.getMessage());
    assertEquals(expectedMessage, actualWithLongitudeOnly.getMessage());
    verifyNoInteractions(serviceMock);
  }

  @Test
  void get_geocode_without_address_nor_coordinates_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.getGeocode(null, null, null));
    var actualWithBlankAddress =
        assertThrows(BadRequestException.class, () -> subject.getGeocode("   ", null, null));

    var expectedMessage = "Either address or point coordinates (longitude,latitude) is required";
    assertEquals(expectedMessage, actual.getMessage());
    assertEquals(expectedMessage, actualWithBlankAddress.getMessage());
    verifyNoInteractions(serviceMock);
  }

  private MultipartFile excelMultipartFile() {
    var multipartFileMock = mock(MultipartFile.class);
    when(multipartFileMock.getOriginalFilename()).thenReturn("addresses.xlsx");
    return multipartFileMock;
  }

  private void authenticateCommunity(String communityId) {
    var apiKey = "dummy-api-key-" + randomUUID();
    when(authProviderMock.getPrincipal())
        .thenReturn(new Principal(apiKey, Set.of(new Authority(ROLE_ADMIN))));
    when(communityAuthorizationRepositoryMock.findByApiKey(apiKey))
        .thenReturn(Optional.of(CommunityAuthorization.builder().id(communityId).build()));
  }

  @Test
  void geocode_excel_addresses_with_provided_sheet_index_ok() {
    var endToEndId = randomUUID().toString();
    var communityId = randomUUID().toString();
    var sheetIndex = 3;
    authenticateCommunity(communityId);
    var domainJob = GeoCodingJob.builder().id(randomUUID().toString()).build();
    var restJob = new app.bpartners.geojobs.endpoint.rest.model.GeoCodingJob().id(endToEndId);
    when(serviceMock.submitGeoCodingJobThroughExcel(
            eq(endToEndId), eq(communityId), any(File.class), eq(sheetIndex)))
        .thenReturn(domainJob);
    when(geoCodingJobRestMapperMock.toRest(domainJob)).thenReturn(restJob);

    var actual =
        subject.geocodeAddressesThroughExcelAddresses(endToEndId, excelMultipartFile(), sheetIndex);

    assertEquals(restJob, actual);
    var sheetIndexCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(serviceMock)
        .submitGeoCodingJobThroughExcel(
            eq(endToEndId), eq(communityId), any(File.class), sheetIndexCaptor.capture());
    assertEquals(sheetIndex, sheetIndexCaptor.getValue());
  }

  @Test
  void geocode_excel_addresses_without_sheet_index_ok() {
    var endToEndId = randomUUID().toString();
    var communityId = randomUUID().toString();
    authenticateCommunity(communityId);
    var domainJob = GeoCodingJob.builder().id(randomUUID().toString()).build();
    var restJob = new app.bpartners.geojobs.endpoint.rest.model.GeoCodingJob().id(endToEndId);
    when(serviceMock.submitGeoCodingJobThroughExcel(
            eq(endToEndId), eq(communityId), any(File.class), eq(null)))
        .thenReturn(domainJob);
    when(geoCodingJobRestMapperMock.toRest(domainJob)).thenReturn(restJob);

    var actual =
        subject.geocodeAddressesThroughExcelAddresses(endToEndId, excelMultipartFile(), null);

    assertEquals(restJob, actual);
    var sheetIndexCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(serviceMock)
        .submitGeoCodingJobThroughExcel(
            eq(endToEndId), eq(communityId), any(File.class), sheetIndexCaptor.capture());
    assertNull(sheetIndexCaptor.getValue());
  }

  @SneakyThrows
  @Test
  void geocode_excel_addresses_binds_plain_text_sheet_index_part_ok() {
    var mockMvc = MockMvcBuilders.standaloneSetup(subject).build();
    var endToEndId = randomUUID().toString();
    var communityId = randomUUID().toString();
    authenticateCommunity(communityId);
    when(serviceMock.submitGeoCodingJobThroughExcel(any(), any(), any(File.class), any()))
        .thenReturn(GeoCodingJob.builder().id(randomUUID().toString()).build());
    when(geoCodingJobRestMapperMock.toRest(any()))
        .thenReturn(new app.bpartners.geojobs.endpoint.rest.model.GeoCodingJob().id(endToEndId));
    var filePart = new MockMultipartFile("file", "addresses.xlsx", null, "dummy".getBytes());
    var sheetIndexPart = new MockPart("sheetIndex", "2".getBytes());

    var actual =
        mockMvc
            .perform(
                multipart("/geoCodingJobs/{id}/excel", endToEndId)
                    .file(filePart)
                    .part(sheetIndexPart))
            .andReturn();

    assertEquals(200, actual.getResponse().getStatus());
    verify(serviceMock)
        .submitGeoCodingJobThroughExcel(eq(endToEndId), eq(communityId), any(File.class), eq(2));
  }

  @SneakyThrows
  @Test
  void geocode_excel_addresses_without_sheet_index_part_ok() {
    var mockMvc = MockMvcBuilders.standaloneSetup(subject).build();
    var endToEndId = randomUUID().toString();
    var communityId = randomUUID().toString();
    authenticateCommunity(communityId);
    when(serviceMock.submitGeoCodingJobThroughExcel(any(), any(), any(File.class), any()))
        .thenReturn(GeoCodingJob.builder().id(randomUUID().toString()).build());
    when(geoCodingJobRestMapperMock.toRest(any()))
        .thenReturn(new app.bpartners.geojobs.endpoint.rest.model.GeoCodingJob().id(endToEndId));
    var filePart = new MockMultipartFile("file", "addresses.xlsx", null, "dummy".getBytes());

    var actual =
        mockMvc
            .perform(multipart("/geoCodingJobs/{id}/excel", endToEndId).file(filePart))
            .andReturn();

    assertEquals(200, actual.getResponse().getStatus());
    verify(serviceMock)
        .submitGeoCodingJobThroughExcel(eq(endToEndId), eq(communityId), any(File.class), eq(null));
  }

  @SneakyThrows
  @Test
  void geocode_excel_addresses_ko_when_file_transfer_fails() {
    var endToEndId = randomUUID().toString();
    authenticateCommunity(randomUUID().toString());
    var multipartFileMock = excelMultipartFile();
    doThrow(IOException.class).when(multipartFileMock).transferTo(any(File.class));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.geocodeAddressesThroughExcelAddresses(endToEndId, multipartFileMock, 2));

    assertEquals(
        "Unable to geocode uploaded file as file extraction exception occurred : addresses.xlsx",
        actual.getMessage());
    verifyNoInteractions(serviceMock);
  }
}
