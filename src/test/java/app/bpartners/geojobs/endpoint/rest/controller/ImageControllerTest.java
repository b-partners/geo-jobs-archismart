package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.ImageUsage.ROOF_DAMAGE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.controller.v1.ImageController;
import app.bpartners.geojobs.endpoint.rest.model.ImageDetails;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.FileApi;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureMapLayer;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.FileType;
import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

class ImageControllerTest {
  AreaPictureApi areaPictureApiMock = mock();
  FileApi fileApiMock = mock();
  final String adminApiKey = randomUUID().toString();

  ImageController subject = new ImageController(areaPictureApiMock, fileApiMock, adminApiKey);

  @BeforeEach
  void setUp() {
    var areaPictureDetailsMock = mock(AreaPictureDetails.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    when(areaPictureMapLayerMock.precisionLevelInCm()).thenReturn(5);
    when(areaPictureDetailsMock.actualLayer()).thenReturn(areaPictureMapLayerMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenReturn(areaPictureDetailsMock);
    when(fileApiMock.downloadOrUploadFile(any(), any(), any())).thenReturn(new byte[0]);
  }

  @Test
  void throw_exception_when_both_address_and_lon_lat_provided() {
    var actualOne =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.getImage(
                    randomUUID().toString(), null, null, null, BigDecimal.ZERO, null, null));
    var actualTwo =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.getImage(
                    randomUUID().toString(), null, null, null, null, BigDecimal.ZERO, null));
    var actualThree =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.getImage(
                    randomUUID().toString(),
                    null,
                    null,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null));

    var expectedExceptionMessage = "Provide either an address or coordinates, not both";
    assertEquals(expectedExceptionMessage, actualOne.getMessage());
    assertEquals(expectedExceptionMessage, actualTwo.getMessage());
    assertEquals(expectedExceptionMessage, actualThree.getMessage());
  }

  @Test
  void throw_exception_when_either_address_or_lon_lat_not_provided() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.getImage(null, null, null, null, null, null, null));

    assertEquals(
        "Either address or both longitude and latitude must be provided", actual.getMessage());
  }

  @Test
  void get_images_ok() {
    var address = "some address";

    var actual = subject.getImage(address, null, null, null, null, null, null);

    var stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(fileApiMock, only())
        .downloadOrUploadFile(stringCaptor.capture(), any(FileType.class), eq(adminApiKey));
    var fileIdGenerated = stringCaptor.getValue();
    assertEquals(
        new ImageDetails().address(address).imageBase64("data:image/jpeg;base64,"), actual);
    assertNotNull(fileIdGenerated);
  }

  @Test
  void get_images_for_detection_usage_ko() {
    reset(areaPictureApiMock);
    var areaPictureDetailsMock = mock(AreaPictureDetails.class);
    var areaPictureMapLayerMock = mock(AreaPictureMapLayer.class);
    when(areaPictureMapLayerMock.precisionLevelInCm()).thenReturn(20);
    when(areaPictureDetailsMock.actualLayer()).thenReturn(areaPictureMapLayerMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenReturn(areaPictureDetailsMock);
    var address = "some random address " + randomUUID();

    var actualException =
        assertThrows(
            NotImplementedException.class,
            () -> subject.getImage(address, null, null, null, null, null, ROOF_DAMAGE_DETECTION));

    assertEquals(
        "Unavailable images for address : "
            + address
            + " and usage ROOF_DAMAGE_DETECTION as image precision level is 20 cm",
        actualException.getMessage());
  }

  @Test
  void capture_crupdate_area_picture_details_when_get_images_ok() {
    var address = "other address";
    var isExtended = true;
    int providedShiftNb = 1;

    assertDoesNotThrow(
        () -> subject.getImage(address, null, isExtended, providedShiftNb, null, null, null));

    var crupdateAreaPictureDetailsCaptor =
        ArgumentCaptor.forClass(CrupdateAreaPictureDetails.class);
    verify(areaPictureApiMock, only())
        .crupdateAreaPictureDetails(
            any(String.class), crupdateAreaPictureDetailsCaptor.capture(), eq(adminApiKey));
    var actual = crupdateAreaPictureDetailsCaptor.getValue();
    var expected = expectedCrupdateAreaPictureDetails(address, actual);
    assertEquals(expected, actual);
  }

  private @NotNull CrupdateAreaPictureDetails expectedCrupdateAreaPictureDetails(
      String address, CrupdateAreaPictureDetails crupdateAreaPictureDetails) {
    int shiftNb = 1;
    boolean isExtended = true;
    String prospectId = null;
    var zoomLevel = HOUSES_0;
    return new CrupdateAreaPictureDetails(
        address,
        shiftNb,
        isExtended,
        crupdateAreaPictureDetails.fileId(),
        crupdateAreaPictureDetails.filename(),
        prospectId,
        zoomLevel);
  }

  @Test
  void get_images_ko() {
    reset(areaPictureApiMock);
    when(areaPictureApiMock.crupdateAreaPictureDetails(any(), any(), any()))
        .thenThrow(
            new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    String notFoundAddress = "not found address";
    var expectedMessageException = "Unable to retrieve image of address : " + notFoundAddress;

    var actual =
        assertThrows(
            ApiException.class,
            () -> subject.getImage(notFoundAddress, null, null, null, null, null, null));

    assertEquals(expectedMessageException, actual.getMessage());
  }
}
