package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
import static java.lang.System.currentTimeMillis;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionConverter
    implements BiFunction<List<Detection>, List<DetectionExportAttribute>, File> {
  private static final String[] HEADERS = {
    "ID", "Adresse ou Nom de zone", "Superficie (m²)", "Date de création"
  };
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
                  var areaInSquareMeters = geometrySquareMeterArea.apply(unifiedProcessedGeoJson);
                  var creationDatetime = detection.getCreationDatetime();
                  return new DetectionExportPayload(
                      e2Id, zoneName, areaInSquareMeters, creationDatetime);
                })
            .toList();

    return export(detectionExportPayloads);
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
                if (featureDelimitations == null) {
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
        .reduce(unifyMultiPolygon())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Unable to combine provided geoJson into unique Feature"));
  }

  @SneakyThrows
  private File export(List<DetectionExportPayload> payloads) {
    File file = Files.createTempFile("detections-" + currentTimeMillis(), ".xlsx").toFile();

    try (Workbook workbook = new XSSFWorkbook();
        FileOutputStream out = new FileOutputStream(file)) {

      Sheet sheet = workbook.createSheet("Detections");
      CreationHelper helper = workbook.getCreationHelper();

      // Styles
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      CellStyle dateStyle = workbook.createCellStyle();
      dateStyle.setDataFormat(helper.createDataFormat().getFormat("dd-mm-yyyy hh:mm:ss"));

      CellStyle areaStyle = workbook.createCellStyle();
      areaStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

      // En-tête
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      // Données
      int rowIndex = 1;
      for (DetectionExportPayload payload : payloads) {
        Row row = sheet.createRow(rowIndex++);

        row.createCell(0).setCellValue(payload.e2Id());
        row.createCell(1).setCellValue(payload.zoneName());

        Cell areaCell = row.createCell(2);
        if (payload.areaInSquareMeters() != null) {
          areaCell.setCellValue(payload.areaInSquareMeters());
          areaCell.setCellStyle(areaStyle);
        }

        Cell dateCell = row.createCell(3);
        if (payload.creationDatetime() != null) {
          dateCell.setCellValue(
              LocalDateTime.ofInstant(payload.creationDatetime(), ZoneId.systemDefault()));
          dateCell.setCellStyle(dateStyle);
        }
      }

      for (int i = 0; i < HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
    }

    return file;
  }

  private record DetectionExportPayload(
      String e2Id, String zoneName, Double areaInSquareMeters, Instant creationDatetime) {}
}
