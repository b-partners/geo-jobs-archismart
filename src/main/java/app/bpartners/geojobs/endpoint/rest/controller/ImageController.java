package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.ImageUsage.ROOF_DAMAGE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.ImageUsage.UNKNOWN;
import static app.bpartners.geojobs.endpoint.rest.model.ImageZoomLevel.*;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.dashboard.component.FileType.AREA_PICTURE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.FileApi;
import app.bpartners.geojobs.service.dashboard.component.AreaPictureDetails;
import app.bpartners.geojobs.service.dashboard.component.CrupdateAreaPictureDetails;
import java.math.BigDecimal;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ImageController {
  private final AreaPictureApi areaPictureApi;
  private final FileApi fileApi;
  private final String adminApiKey;

  public ImageController(
      AreaPictureApi areaPictureApi,
      FileApi fileApi,
      @Value("${admin.api.key}") String adminApiKey) {
    this.areaPictureApi = areaPictureApi;
    this.fileApi = fileApi;
    this.adminApiKey = adminApiKey;
  }

  @GetMapping("/image")
  public ImageDetails getImage(
      @RequestParam(required = false) String address,
      @RequestParam(required = false, name = "zoom") ImageZoomLevel zoom,
      @RequestParam(required = false) Boolean isExtended,
      @RequestParam(required = false, name = "shiftNb") Integer providedShiftNb,
      @RequestParam(required = false, name = "longitude") BigDecimal longitude,
      @RequestParam(required = false, name = "latitude") BigDecimal latitude,
      @RequestParam(required = false, name = "usage") ImageUsage usage) {
    boolean hasAddress = checkAddressAndPointCoordinates(address, longitude, latitude);

    if (!hasAddress) {
      address = latitude.doubleValue() + "," + longitude.doubleValue();
    }

    usage = usage == null ? UNKNOWN : usage;

    var requestedZoom = getZoomLevelEnum(zoom);
    var areaPictureId = randomUUID().toString();
    var fileId = randomUUID().toString();
    var areaPictureDetails =
        getAreaPictureDetails(
            address, isExtended, providedShiftNb, usage, areaPictureId, fileId, requestedZoom);
    try {
      byte[] imageAsBytes = fileApi.downloadOrUploadFile(fileId, AREA_PICTURE, adminApiKey);
      return new ImageDetails()
          .minTileCoordinates(
              areaPictureDetails.referenceTile() == null
                  ? null
                  : new TileCoordinates()
                      .x(areaPictureDetails.referenceTile().x())
                      .y(areaPictureDetails.referenceTile().y())
                      .z(areaPictureDetails.referenceTile().zoom().number()))
          .currentGeoPosition(
              areaPictureDetails.currentGeoPosition() == null
                  ? null
                  : new GeoPosition()
                      .latitude(
                          latitude != null
                              ? latitude
                              : BigDecimal.valueOf(
                                  areaPictureDetails.currentGeoPosition().latitude()))
                      .longitude(
                          longitude != null
                              ? longitude
                              : BigDecimal.valueOf(
                                  areaPictureDetails.currentGeoPosition().longitude())))
          .address((longitude != null || latitude != null) ? null : address)
          .zoomLevel(zoom)
          .imageBase64(
              "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageAsBytes));
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ApiException(SERVER_EXCEPTION, "Unable to retrieve image of address : " + address);
    }
  }

  @NotNull
  private AreaPictureDetails getAreaPictureDetails(
      String address,
      Boolean isExtended,
      Integer providedShiftNb,
      ImageUsage usage,
      String areaPictureId,
      String fileId,
      ZoneTilingJob.ZoomLevelEnum requestedZoom) {
    AreaPictureDetails areaPictureDetails;
    try {
      var shiftNb = providedShiftNb == null ? 0 : providedShiftNb;
      areaPictureDetails =
          areaPictureApi.crupdateAreaPictureDetails(
              areaPictureId,
              new CrupdateAreaPictureDetails(
                  address,
                  shiftNb,
                  isExtended == null || isExtended,
                  fileId,
                  address + randomUUID(),
                  null,
                  requestedZoom),
              adminApiKey);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      throw new ApiException(SERVER_EXCEPTION, "Unable to retrieve image of address : " + address);
    }
    var precisionLevelInCm = areaPictureDetails.actualLayer().precisionLevelInCm();
    if (precisionLevelInCm != 5 && ROOF_DAMAGE_DETECTION.equals(usage)) {
      throw new NotImplementedException(
          "Unavailable images for address : "
              + address
              + " and usage ROOF_DAMAGE_DETECTION as image precision level is "
              + precisionLevelInCm
              + " cm");
    }
    return areaPictureDetails;
  }

  private boolean checkAddressAndPointCoordinates(
      String address, BigDecimal longitude, BigDecimal latitude) {
    boolean hasAddress = address != null;
    boolean hasCoordinates = longitude != null && latitude != null;
    boolean hasPartialCoordinates = longitude != null || latitude != null;

    if (hasAddress && hasPartialCoordinates) {
      throw new BadRequestException("Provide either an address or coordinates, not both");
    }

    if (!hasAddress && !hasCoordinates) {
      throw new BadRequestException(
          "Either address or both longitude and latitude must be provided");
    }
    return hasAddress;
  }

  private ZoneTilingJob.ZoomLevelEnum getZoomLevelEnum(ImageZoomLevel zoom) {
    if (zoom == null) {
      return ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
    }
    return switch (zoom) {
      case BUILDING -> ZoneTilingJob.ZoomLevelEnum.HOUSES_0;
      case NEIGHBORHOOD -> ZoneTilingJob.ZoomLevelEnum.BUILDING;
      case DISTRICT -> ZoneTilingJob.ZoomLevelEnum.BUILDINGS;
    };
  }
}
