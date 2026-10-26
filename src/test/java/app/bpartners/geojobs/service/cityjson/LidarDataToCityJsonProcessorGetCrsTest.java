package app.bpartners.geojobs.service.cityjson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.lidar.planes.model.DelimitedRoofPoints;
import app.bpartners.geojobs.service.cityjson.factory.CityJsonFactory;
import app.bpartners.geojobs.service.lidar.PointsExtractionResult;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.service.lidar.api.WalloniaBoundaryChecker;
import java.lang.reflect.Method;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

class LidarDataToCityJsonProcessorGetCrsTest {
  SwissBoundaryChecker swissBoundaryCheckerMock = mock(SwissBoundaryChecker.class);
  WalloniaBoundaryChecker walloniaBoundaryCheckerMock = mock(WalloniaBoundaryChecker.class);
  LidarDataToCityJsonProcessor subject =
      new LidarDataToCityJsonProcessor(
          new CityJsonFactory(), swissBoundaryCheckerMock, walloniaBoundaryCheckerMock);

  @Test
  void geometry_in_switzerland_returns_epsg_2056() {
    var geometryWGS84 = mock(Geometry.class);
    when(swissBoundaryCheckerMock.isGeometryInSwiss(geometryWGS84)).thenReturn(true);
    when(walloniaBoundaryCheckerMock.isGeometryInWallonia(any())).thenReturn(false);

    var actual = getCrs(pointsExtractionResultOf(geometryWGS84));

    assertEquals("EPSG:2056", actual);
  }

  @Test
  void geometry_in_wallonia_returns_epsg_3812() {
    var geometryWGS84 = mock(Geometry.class);
    when(swissBoundaryCheckerMock.isGeometryInSwiss(geometryWGS84)).thenReturn(false);
    when(walloniaBoundaryCheckerMock.isGeometryInWallonia(geometryWGS84)).thenReturn(true);

    var actual = getCrs(pointsExtractionResultOf(geometryWGS84));

    assertEquals("EPSG:3812", actual);
  }

  @Test
  void geometry_outside_switzerland_and_wallonia_returns_epsg_2154() {
    var geometryWGS84 = mock(Geometry.class);
    when(swissBoundaryCheckerMock.isGeometryInSwiss(geometryWGS84)).thenReturn(false);
    when(walloniaBoundaryCheckerMock.isGeometryInWallonia(geometryWGS84)).thenReturn(false);

    var actual = getCrs(pointsExtractionResultOf(geometryWGS84));

    assertEquals("EPSG:2154", actual);
  }

  @Test
  void no_roof_data_returns_epsg_2154() {
    var actual = getCrs(new PointsExtractionResult(Map.of()));

    assertEquals("EPSG:2154", actual);
  }

  private static PointsExtractionResult pointsExtractionResultOf(Geometry geometryWGS84) {
    var roofPoints = mock(DelimitedRoofPoints.class);
    when(roofPoints.getOriginalInEPSG4336()).thenReturn(geometryWGS84);
    return new PointsExtractionResult(Map.of(new Envelope(0, 1, 0, 1), roofPoints));
  }

  @SneakyThrows
  private String getCrs(PointsExtractionResult result) {
    Method method =
        LidarDataToCityJsonProcessor.class.getDeclaredMethod(
            "getCrs", PointsExtractionResult.class);
    method.setAccessible(true);
    return (String) method.invoke(subject, result);
  }
}
