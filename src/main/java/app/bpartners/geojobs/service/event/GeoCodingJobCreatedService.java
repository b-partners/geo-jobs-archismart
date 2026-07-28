package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.FAILED;
import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.SUCCEEDED;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.GeoCodingJobCreated;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoCodingJobRepository;
import app.bpartners.geojobs.service.ExcelAddressConverter;
import app.bpartners.geojobs.service.GeoCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoCodingJobCreatedService implements Consumer<GeoCodingJobCreated> {
  private final GeoCodingJobRepository repository;
  private final ExcelAddressConverter excelAddressConverter;
  private final BucketComponent bucketComponent;
  private final GeoCodeService geoCodeService;
  private final ObjectMapper objectMapper;

  @Override
  public void accept(GeoCodingJobCreated geoCodingJobCreated) {
    var geoCodingJobIdentifier = geoCodingJobCreated.getGeoCodingJobIdentifier();
    var geoCodingJob = repository.findById(geoCodingJobIdentifier).orElseThrow();
    var sheetIndex = geoCodingJob.getSheetIndex();
    try {
      var fileKey = geoCodingJob.getFileKey();
      var downloadedExcelFile = bucketComponent.download(fileKey);
      var fullTextAddresses = excelAddressConverter.apply(downloadedExcelFile, sheetIndex);
      log.info(
          "Processing GeoCodingJob(id={}, fileKey={}, sheetIndex={}) holding {} address(es)",
          geoCodingJobIdentifier,
          fileKey,
          sheetIndex,
          fullTextAddresses.size());
      var unprocessedAddresses = new ArrayList<Map<String, String>>();
      var convertedRestFeatures =
          fullTextAddresses.stream()
              .map(
                  address -> {
                    try {
                      return geoCodeService.geocode(address);
                    } catch (Exception e) {
                      log.warn(
                          "Skipping address={} of GeoCodingJob(id={}) as geocoding failed",
                          address,
                          geoCodingJobIdentifier,
                          e);
                      var unprocessedAddress = new HashMap<String, String>();
                      unprocessedAddress.put(address, e.getMessage());
                      unprocessedAddresses.add(unprocessedAddress);
                      var properties = new HashMap<String, Object>();
                      properties.put("address", address);
                      return app.bpartners.geojobs.repository.model.Feature.builder()
                          .geometry(
                              new app.bpartners.geojobs.repository.model.Feature.FeatureGeometry())
                          .properties(properties)
                          .build();
                    }
                  })
              .map(this::toRestFeatureLoggingFailingAddress)
              .toList();

      var tempFile = writeRestFeaturesInsideTmpFile(convertedRestFeatures);

      var geoJsonFileKey =
          "geocoding/" + geoCodingJob.getId() + "/geoJson/" + randomUUID() + ".geojson";

      bucketComponent.upload(tempFile, geoJsonFileKey);

      var geoCodingJobBuilder =
          geoCodingJob.toBuilder().geoJsonKey(geoJsonFileKey).status(SUCCEEDED);
      if (!unprocessedAddresses.isEmpty()) {
        var errorMessage = buildErrorMessage(unprocessedAddresses);
        geoCodingJobBuilder.message(errorMessage);
      }

      repository.save(geoCodingJobBuilder.build());
    } catch (Exception e) {
      log.error("Exception on processing GeoCodingJob(id={})", geoCodingJobIdentifier, e);
      repository.save(geoCodingJob.toBuilder().message(e.getMessage()).status(FAILED).build());
    }
  }

  private Feature toRestFeatureLoggingFailingAddress(
      app.bpartners.geojobs.repository.model.Feature domainFeature) {
    try {
      return FeatureMapper.toRestFeature(domainFeature);
    } catch (RuntimeException e) {
      var properties = domainFeature.getProperties();
      log.error(
          "Unable to convert geocoded Feature of address={} to rest Feature, its geometry is {}",
          properties == null ? null : properties.get("address"),
          domainFeature.getGeometry(),
          e);
      throw e;
    }
  }

  private String buildErrorMessage(ArrayList<Map<String, String>> errors) {
    if (errors == null || errors.isEmpty()) {
      return "";
    }

    return errors.stream()
        .filter(Objects::nonNull)
        .flatMap(map -> map.entrySet().stream())
        .map(
            entry ->
                String.format(
                    "%s : %s",
                    entry.getKey(),
                    entry.getValue() == null ? "unknown exception" : entry.getValue()))
        .collect(Collectors.joining(" | "));
  }

  @NotNull
  private File writeRestFeaturesInsideTmpFile(List<Feature> convertedRestFeatures)
      throws IOException {
    var tempFile = File.createTempFile(randomUUID().toString(), ".geojson");
    objectMapper.writeValue(tempFile, convertedRestFeatures);
    return tempFile;
  }
}
