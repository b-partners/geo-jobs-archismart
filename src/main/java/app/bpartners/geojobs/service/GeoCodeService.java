package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoCodingJobCreated;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.GeoCodingJobRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import com.google.maps.errors.ApiException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoCodeService {
  private final GeoCodeApi geoCodeApi;
  private final GeometryConverter geometryConverter;
  private final GeoCodingJobRepository repository;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;

  public GeoCodingJob submitGeoCodingJobThroughExcel(
      String endToEndId, String communityOwnerId, File excelFile) {
    var optionalGeoCodingJob =
        repository.findByEndToEndIdAndCommunityOwnerId(endToEndId, communityOwnerId);
    if (optionalGeoCodingJob.isPresent()) {
      throw new BadRequestException(
          "Processed GeoCodingJob(id=" + endToEndId + ") can not be updated");
    }
    var geoCodingJobId = randomUUID().toString();
    var bucketKey = "geocoding/" + geoCodingJobId + ".xlsx";

    bucketComponent.upload(excelFile, bucketKey);

    var geoCodingJob =
        GeoCodingJob.builder()
            .id(geoCodingJobId)
            .endToEndId(endToEndId)
            .communityOwnerId(communityOwnerId)
            .fileKey(bucketKey)
            .geoJsonKey(null)
            .status(PROCESSING)
            .creationDatetime(now())
            .build();
    var savedGeoCodingJob = repository.save(geoCodingJob);

    eventProducer.accept(List.of(new GeoCodingJobCreated(savedGeoCodingJob.getId())));

    return savedGeoCodingJob;
  }

  public GeoCodingJob findByEndToEndIdAndCommunityOwnerId(String e2Id, String communityOwnerId) {
    var optionalGeoCodingJob =
        repository.findByEndToEndIdAndCommunityOwnerId(e2Id, communityOwnerId);
    if (optionalGeoCodingJob.isEmpty()) {
      throw new NotFoundException(String.format("GeoCodingJob with id %s not found", e2Id));
    }
    return optionalGeoCodingJob.get();
  }

  public Feature geocode(String address) {
    if (address == null || address.isBlank()) {
      throw new BadRequestException("Address is mandatory");
    }
    try {
      var geoPosition = geoCodeApi.searchGeoPositionFromAddress(address);
      var longitude = BigDecimal.valueOf(geoPosition.longitude());
      var latitude = BigDecimal.valueOf(geoPosition.latitude());
      var nearestRoofMultiPolygon =
          geometryConverter.retrieveNearestRoofMultiPolygon(List.of(longitude, latitude));

      var properties = new HashMap<String, Object>();
      properties.put("address", address);

      return geometryConverter.toFeature(
          null, HOUSES_0.getZoomLevel(), properties, nearestRoofMultiPolygon);
    } catch (IOException | InterruptedException | ApiException e) {
      throw new BadRequestException("Unable to geocode address : " + address);
    }
  }
}
