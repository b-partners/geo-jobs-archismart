package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.GeoCodingJobRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.GeoCodeService;
import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class GeoCodeController {
  private final GeoCodeService service;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final AuthProvider authProvider;
  private final GeoCodingJobRestMapper geoCodingJobRestMapper;

  @GetMapping("/geocode")
  public Feature getGeocode(@RequestParam("address") String address) {
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
      @PathVariable(name = "id") String id, @RequestPart(value = "file") MultipartFile file) {
    try {
      var tempFile = File.createTempFile("geocoding-addresses-", file.getOriginalFilename());
      file.transferTo(tempFile);
      return geoCodingJobRestMapper.toRest(
          service.submitGeoCodingJobThroughExcel(id, getCommunityAuthorizationId(), tempFile));
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
