package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.FAILED;
import static app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus.SUCCEEDED;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.GeoCodingJobCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.GeoCodingJobRepository;
import app.bpartners.geojobs.service.ExcelAddressConverter;
import app.bpartners.geojobs.service.GeoCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
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
    try {
      var fileKey = geoCodingJob.getFileKey();
      var downloadedExcelFile = bucketComponent.download(fileKey);
      var fullTextAddresses = excelAddressConverter.apply(downloadedExcelFile);

      var convertedRestFeatures =
          fullTextAddresses.stream()
              .map(geoCodeService::geocode)
              .map(FeatureMapper::toRestFeature)
              .toList();

      var tempFile = writeRestFeaturesInsideTmpFile(convertedRestFeatures);

      var geoJsonFileKey =
          "geocoding/" + geoCodingJob.getId() + "/geoJson/" + randomUUID() + ".geojson";

      bucketComponent.upload(tempFile, geoJsonFileKey);

      repository.save(
          geoCodingJob.toBuilder().geoJsonKey(geoJsonFileKey).status(SUCCEEDED).build());
    } catch (Exception e) {
      log.error(
          "Exception on processing GeoCodingJob(id={}) : {} ",
          geoCodingJobIdentifier,
          e.getMessage());
      repository.save(geoCodingJob.toBuilder().status(FAILED).build());
    }
  }

  @NotNull
  private File writeRestFeaturesInsideTmpFile(List<Feature> convertedRestFeatures)
      throws IOException {
    var tempFile = File.createTempFile(randomUUID().toString(), ".geojson");
    objectMapper.writeValue(tempFile, convertedRestFeatures);
    return tempFile;
  }
}
