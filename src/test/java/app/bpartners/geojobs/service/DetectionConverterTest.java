package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;

class DetectionConverterTest {
  GeometrySquareMeterArea geometrySquareMeterAreaMock = mock();
  BuildingFinder buildingFinderMock = mock();
  GeometryConverter geometryConverter = new GeometryConverter();

  DetectionConverter subject =
      new DetectionConverter(geometrySquareMeterAreaMock, geometryConverter, buildingFinderMock);

  GeometryFactory geometryFactory = new GeometryFactory();

  Detection pointOnlyDetection() {
    var point = geometryFactory.createPoint(new Coordinate(2.35, 48.85));
    var pointFeature = geometryConverter.toFeature("feature_id", 20, Map.of(), point);
    return Detection.builder()
        .id("detection_id")
        .endToEndId("e2e_id")
        .zoneName("1 rue de Paris")
        .providedGeoJsonZone(List.of(pointFeature))
        .build();
  }

  MultiPolygon building() {
    var ring =
        geometryFactory.createLinearRing(
            new Coordinate[] {
              new Coordinate(2.35, 48.85),
              new Coordinate(2.351, 48.85),
              new Coordinate(2.351, 48.851),
              new Coordinate(2.35, 48.85)
            });
    return geometryFactory.createMultiPolygon(
        new org.locationtech.jts.geom.Polygon[] {geometryFactory.createPolygon(ring)});
  }

  @Test
  void export_area_of_point_only_detection_from_building_finder() throws Exception {
    var building = building();
    when(buildingFinderMock.getBuildingMultiPolygon(any(Point.class))).thenReturn(building);
    when(geometrySquareMeterAreaMock.apply(building)).thenReturn(123.45);

    var file = subject.apply(List.of(pointOnlyDetection()), List.of());

    try (var workbook = new XSSFWorkbook(new FileInputStream(file))) {
      var row = workbook.getSheetAt(0).getRow(1);
      assertEquals("e2e_id", row.getCell(0).getStringCellValue());
      assertEquals(123.45, row.getCell(2).getNumericCellValue());
    }
  }

  @Test
  void export_blank_area_when_no_building_found_for_point_only_detection() throws Exception {
    when(buildingFinderMock.getBuildingMultiPolygon(any(Point.class))).thenReturn(null);

    var file = subject.apply(List.of(pointOnlyDetection()), List.of());

    try (var workbook = new XSSFWorkbook(new FileInputStream(file))) {
      var row = workbook.getSheetAt(0).getRow(1);
      assertEquals("e2e_id", row.getCell(0).getStringCellValue());
      assertEquals("BLANK", row.getCell(2).getCellType().name());
    }
  }
}
