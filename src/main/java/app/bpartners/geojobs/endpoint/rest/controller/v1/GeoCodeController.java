package app.bpartners.geojobs.endpoint.rest.controller.v1;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import app.bpartners.geojobs.endpoint.rest.V1RestController;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.GeoCodingJobRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.GeoCodeService;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@V1RestController
@RequiredArgsConstructor
public class GeoCodeController {
  private final GeoCodeService service;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final AuthProvider authProvider;
  private final GeoCodingJobRestMapper geoCodingJobRestMapper;

  @GetMapping("/geocode")
  public Feature getGeocode(
      @RequestParam(value = "address", required = false) String address,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {
    var hasAddress = address != null && !address.isBlank();
    var hasCoordinates = latitude != null || longitude != null;
    if (hasAddress && hasCoordinates) {
      throw new BadRequestException(
          "Both address and point coordinates (longitude,latitude) can not be provided");
    }
    if (!hasAddress && !hasCoordinates) {
      throw new BadRequestException(
          "Either address or point coordinates (longitude,latitude) is required");
    }
    if (!hasAddress) {
      if (longitude == null || latitude == null) {
        throw new BadRequestException(
            "Both longitude and latitude are required to geocode from point coordinates");
      }
      return toRestFeature(
          service.geocode(null, BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude)));
    }
    return toRestFeature(service.geocode(address));
  }

  @GetMapping("/geoCodingJobs/{id}")
  public GeoCodingJob retrieveGeoCodingJobById(@PathVariable(name = "id") String id) {
    var geoCodingJob =
        service.findByEndToEndIdAndCommunityOwnerId(id, getCommunityAuthorizationId());
    return geoCodingJobRestMapper.toRest(geoCodingJob);
  }

  @PostMapping(value = "/geoCodingJobs/{id}/excel", consumes = MULTIPART_FORM_DATA_VALUE)
  public GeoCodingJob geocodeAddressesThroughExcelAddresses(
      @PathVariable(name = "id") String id,
      @RequestPart(value = "file") MultipartFile file,
      @RequestParam(value = "sheetIndex", required = false) Integer sheetIndex) {
    try {
      var tempFile = File.createTempFile("geocoding-addresses-", file.getOriginalFilename());
      file.transferTo(tempFile);
      return geoCodingJobRestMapper.toRest(
          service.submitGeoCodingJobThroughExcel(
              id, getCommunityAuthorizationId(), tempFile, sheetIndex));
    } catch (IOException e) {
      throw new BadRequestException(
          "Unable to geocode uploaded file as file extraction exception occurred : "
              + file.getOriginalFilename());
    }
  }

  private String getCommunityAuthorizationId() {
    var communityAuthorization =
        communityAuthorizationRepository.findByApiKey(authProvider.getPrincipal().getPassword());
    return communityAuthorization.map(CommunityAuthorization::getId).orElse(null);
  }
}
