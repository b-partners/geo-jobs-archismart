package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionConverter
    implements BiFunction<List<Detection>, List<DetectionExportAttribute>, File> {
  private final GeometrySquareMeterArea geometrySquareMeterArea;
  private final GeometryConverter geometryConverter;
  private final BuildingFinder buildingFinder;

  @Override
  public File apply(
      List<Detection> detections, List<DetectionExportAttribute> additionalAttributes) {
    var detectionExportPayloads =
        detections.stream()
            .map(
                detection -> {
                  var unifiedProcessedGeoJson = getUnifiedProcessedGeoJson(detection);
                  var e2Id = detection.getEndToEndId();
                  var zoneName = detection.getZoneName();
                  var areaInSquareMeters =
                      unifiedProcessedGeoJson == null
                          ? null
                          : geometrySquareMeterArea.apply(unifiedProcessedGeoJson);
                  var creationDatetime = detection.getCreationDatetime();
                  return new ExportExcelWriter.ExportRow(
                      e2Id,
                      zoneName,
                      areaInSquareMeters,
                      creationDatetime,
                      detection.exportStatus());
                })
            .toList();

    return ExportExcelWriter.write(detectionExportPayloads);
  }

  private org.locationtech.jts.geom.MultiPolygon getUnifiedProcessedGeoJson(Detection detection) {
    return detection.getProvidedGeoJsonZone().stream()
        .map(
            feature -> {
              var geometry = feature.getGeometry();
              if (geometry.getActualInstance() instanceof Polygon polygon) {
                return geometryConverter.apply(Collections.singletonList(polygon.getCoordinates()));
              } else if (geometry.getActualInstance() instanceof MultiPolygon multiPolygon) {
                return geometryConverter.apply(multiPolygon.getCoordinates());
              } else if (geometry.getActualInstance() instanceof Point point) {
                var featureDelimitations = detection.getDelimitationOf(feature);
                if (featureDelimitations == null || featureDelimitations.isEmpty()) {
                  return buildingFinder.getBuildingMultiPolygon(point);
                }
                return featureDelimitations.stream()
                    .map(
                        featureDelimitation -> {
                          var actualInstance =
                              featureDelimitation.getGeometry().getActualInstance();
                          if (actualInstance instanceof Polygon polygon) {
                            return geometryConverter.apply(
                                Collections.singletonList(polygon.getCoordinates()));
                          } else if (actualInstance instanceof MultiPolygon multiPolygon) {
                            return geometryConverter.apply(multiPolygon.getCoordinates());
                          } else {
                            throw new IllegalStateException(
                                "Unexpected geometry type: " + actualInstance.getClass());
                          }
                        })
                    .reduce(unifyMultiPolygon())
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "Unable to convert provided Point into unique Feature"));
              }
              throw new IllegalStateException(
                  "Unexpected geometry type: " + geometry.getActualInstance().getClass());
            })
        .filter(Objects::nonNull)
        .reduce(unifyMultiPolygon())
        .orElse(null);
  }
}
