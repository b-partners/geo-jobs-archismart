package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.retrieveMultiPolygonFromFeature;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.rest.model.DetectionExportAttribute;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityJSONRequestConverter
    implements BiFunction<List<CityJSONRequest>, List<DetectionExportAttribute>, File> {
  private final GeometrySquareMeterArea geometrySquareMeterArea;

  @Override
  public File apply(
      List<CityJSONRequest> requests, List<DetectionExportAttribute> additionalAttributes) {
    var rows =
        requests.stream()
            .map(
                request ->
                    new ExportExcelWriter.ExportRow(
                        request.getId(),
                        getName(request),
                        getAreaInSquareMeters(request),
                        request.getCreationDatetime(),
                        request.exportStatus()))
            .toList();

    return ExportExcelWriter.write(rows);
  }

  private String getName(CityJSONRequest request) {
    var addresses =
        Stream.concat(
                request.getFeaturesWithDelimitation() == null
                    ? Stream.empty()
                    : request.getFeaturesWithDelimitation().stream()
                        .map(FeatureWithDelimitation::feature),
                request.getDelimitations() == null
                    ? Stream.empty()
                    : request.getDelimitations().stream())
            .filter(Objects::nonNull)
            .map(Feature::getProperties)
            .filter(Objects::nonNull)
            .map(properties -> properties.get("address"))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .distinct()
            .toList();
    if (!addresses.isEmpty()) {
      return String.join(", ", addresses);
    }
    return request.getExternalReference();
  }

  private Double getAreaInSquareMeters(CityJSONRequest request) {
    var delimitations = request.getRequestDelimitations();
    if (delimitations == null) {
      return null;
    }
    return delimitations.stream()
        .map(feature -> toRestFeature(feature))
        .filter(Objects::nonNull)
        .map(feature -> retrieveMultiPolygonFromFeature(feature, null))
        .filter(Objects::nonNull)
        .reduce(unifyMultiPolygon())
        .map(geometrySquareMeterArea)
        .orElse(null);
  }
}
