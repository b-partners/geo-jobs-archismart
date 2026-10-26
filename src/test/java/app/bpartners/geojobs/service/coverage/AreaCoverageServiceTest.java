package app.bpartners.geojobs.service.coverage;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import app.bpartners.geojobs.service.geodata.GeodataMapLayerClient;
import app.bpartners.geojobs.service.geodata.MapLayerResolver;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.geodata.model.MapLayerActual;
import app.bpartners.geojobs.service.lidar.api.GeodataLidarApiClient;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.mockito.ArgumentCaptor;

class AreaCoverageServiceTest {
  GeodataMapLayerClient mapLayerClientMock = mock();
  GeodataLidarApiClient lidarApiClientMock = mock();
  LidarFileChecker lidarFileCheckerMock = mock();
  SupportedModelResolver supportedModelResolverMock = mock();
  AreaCoverageService subject =
      new AreaCoverageService(
          new MapLayerResolver(mapLayerClientMock, mock()),
          lidarApiClientMock,
          lidarFileCheckerMock,
          new CoverageConf(5),
          supportedModelResolverMock);

  @BeforeEach
  void setUp() {
    when(lidarFileCheckerMock.check(any())).thenReturn(LidarFileStatus.VALID);
    when(supportedModelResolverMock.supportedModels(5)).thenReturn(List.of(ModelName.TOITURE));
  }

  private static MapLayerActual actualWithPrecision(Integer precisionInCm) {
    return new MapLayerActual(
        "https://wms",
        new GeodataMapLayer("id", "layer", 2024, "GEOSERVER", "75", "HOUSES_0", precisionInCm));
  }

  @Test
  void check_2d_is_covered_and_hd_when_precision_is_5_cm() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(5)));

    var actual = subject.check2D(2.3, 48.5);

    assertEquals(
        new Coverage2D(
            true,
            true,
            new ImageryLayer("layer", 2024, "GEOSERVER", 5),
            List.of(ModelName.TOITURE)),
        actual);
  }

  @Test
  void check_2d_is_covered_but_not_hd_when_precision_is_over_threshold() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(10)));

    var actual = subject.check2D(2.3, 48.5);

    assertEquals(true, actual.covered());
    assertEquals(false, actual.hd());
  }

  @Test
  void check_2d_is_not_hd_when_precision_is_unknown() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(null)));

    var actual = subject.check2D(2.3, 48.5);

    assertEquals(true, actual.covered());
    assertEquals(false, actual.hd());
  }

  @Test
  void check_2d_is_not_covered_when_no_layer_is_reachable() {
    when(mapLayerClientMock.getActualMapLayer(0.0, 0.0)).thenReturn(Optional.empty());

    assertEquals(Coverage2D.notCovered(), subject.check2D(0.0, 0.0));
  }

  @Test
  void check_2d_propagates_gateway_timeout_instead_of_reporting_not_covered() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenThrow(new GatewayTimeoutException("down"));

    assertThrows(GatewayTimeoutException.class, () -> subject.check2D(2.3, 48.5));
  }

  private static Geometry square(double west, double south) {
    return geometryFactory.createPolygon(
        new Coordinate[] {
          new Coordinate(west, south),
          new Coordinate(west + 0.01, south),
          new Coordinate(west + 0.01, south + 0.01),
          new Coordinate(west, south + 0.01),
          new Coordinate(west, south)
        });
  }

  @Test
  void check_3d_point_is_covered_when_lidar_files_are_found() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("a.laz", "b.laz"))));

    var actual = subject.check3D(2.3, 48.5);

    assertEquals(new Coverage3D(true, "FRANCE", "EPSG:2154", 2, 1, 1), actual);
    var captor = ArgumentCaptor.forClass(List.class);
    verify(lidarApiClientMock).getLidarFileUrls(captor.capture());
    var footprint = (Geometry) captor.getValue().getFirst();
    assertTrue(footprint.contains(geometryFactory.createPoint(new Coordinate(2.3, 48.5))));
  }

  @Test
  void check_3d_point_is_not_covered_when_no_lidar_file_is_found() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult("FRANCE", "EPSG:2154", List.of(Set.of())));

    var actual = subject.check3D(2.3, 48.5);

    assertEquals(new Coverage3D(false, "FRANCE", "EPSG:2154", 0, 0, 1), actual);
  }

  @Test
  void check_3d_zone_is_partially_covered_and_counts_distinct_files() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE",
                "EPSG:2154",
                List.of(Set.of("a.laz", "b.laz"), Set.of("b.laz"), Set.of())));

    var actual = subject.check3D(List.of(square(2, 48), square(3, 48), square(4, 48)));

    assertEquals(new Coverage3D(false, "FRANCE", "EPSG:2154", 2, 2, 3), actual);
  }

  @Test
  void check_3d_zone_is_covered_when_every_geometry_has_files() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "SWITZERLAND", "EPSG:2056", List.of(Set.of("a.laz"), Set.of("b.laz"))));

    var actual = subject.check3D(List.of(square(7, 46), square(8, 46)));

    assertEquals(new Coverage3D(true, "SWITZERLAND", "EPSG:2056", 2, 2, 2), actual);
  }

  @Test
  void check_3d_counts_missing_results_as_not_covered() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("a.laz"))));

    var actual = subject.check3D(List.of(square(2, 48), square(3, 48)));

    assertEquals(new Coverage3D(false, "FRANCE", "EPSG:2154", 1, 1, 2), actual);
  }

  @Test
  void check_3d_rejects_empty_geometries() {
    assertThrows(BadRequestException.class, () -> subject.check3D(List.of()));
  }

  @Test
  void check_3d_propagates_gateway_timeout_instead_of_reporting_not_covered() {
    when(lidarApiClientMock.getLidarFileUrls(any())).thenThrow(new GatewayTimeoutException("down"));

    assertThrows(GatewayTimeoutException.class, () -> subject.check3D(2.3, 48.5));
  }

  @Test
  void check_3d_is_not_covered_when_geodata_files_are_absent_or_invalid() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("gone.laz"), Set.of("bad.laz"))));
    when(lidarFileCheckerMock.check("gone.laz")).thenReturn(LidarFileStatus.ABSENT);
    when(lidarFileCheckerMock.check("bad.laz")).thenReturn(LidarFileStatus.INVALID);

    var actual = subject.check3D(List.of(square(2, 48), square(3, 48)));

    assertEquals(new Coverage3D(false, "FRANCE", "EPSG:2154", 2, 0, 2), actual);
  }

  @Test
  void check_3d_is_covered_when_at_least_one_file_of_the_geometry_is_valid() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("gone.laz", "ok.laz"))));
    when(lidarFileCheckerMock.check("gone.laz")).thenReturn(LidarFileStatus.ABSENT);

    var actual = subject.check3D(2.3, 48.5);

    assertEquals(new Coverage3D(true, "FRANCE", "EPSG:2154", 2, 1, 1), actual);
  }

  @Test
  void check_3d_checks_each_distinct_file_only_once() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("shared.laz"), Set.of("shared.laz"))));

    subject.check3D(List.of(square(2, 48), square(3, 48)));

    verify(lidarFileCheckerMock, times(1)).check("shared.laz");
  }

  @Test
  void check_3d_does_not_report_not_covered_when_a_file_cannot_be_verified() {
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("flaky.laz"))));
    when(lidarFileCheckerMock.check("flaky.laz")).thenReturn(LidarFileStatus.UNKNOWN);

    assertThrows(GatewayTimeoutException.class, () -> subject.check3D(2.3, 48.5));
  }

  @Test
  void check_3d_rejects_null_geometries() {
    assertThrows(BadRequestException.class, () -> subject.check3D((List<Geometry>) null));
  }

  @Test
  void check_2d_is_not_covered_when_geodata_answers_without_layer() {
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(new MapLayerActual("https://wms", null)));

    assertEquals(Coverage2D.notCovered(), subject.check2D(2.3, 48.5));
  }

  @Test
  void check_runs_every_check_when_no_type_is_given() {
    var location = new CoverageLocation(2.3, 48.5);
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(5)));
    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("a.laz"))));

    var withEmptyTypes = subject.check(location, Set.of());
    var withNullTypes = subject.check(location, null);

    assertEquals(withEmptyTypes, withNullTypes);
    assertEquals(location, withEmptyTypes.location());
    assertTrue(withEmptyTypes.imagery().covered());
    assertTrue(withEmptyTypes.lidar().covered());
  }

  @Test
  void check_runs_only_the_requested_types() {
    var location = new CoverageLocation(2.3, 48.5);
    when(mapLayerClientMock.getActualMapLayer(48.5, 2.3))
        .thenReturn(Optional.of(actualWithPrecision(5)));

    var imageryOnly = subject.check(location, Set.of(CoverageType.IMAGERY));

    assertTrue(imageryOnly.imagery().covered());
    assertNull(imageryOnly.lidar());
    verifyNoInteractions(lidarApiClientMock);

    when(lidarApiClientMock.getLidarFileUrls(any()))
        .thenReturn(
            new GeodataLidarApiClient.LidarUrlsResult(
                "FRANCE", "EPSG:2154", List.of(Set.of("a.laz"))));

    var lidarOnly = subject.check(location, Set.of(CoverageType.LIDAR));

    assertNull(lidarOnly.imagery());
    assertNotNull(lidarOnly.lidar());
  }
}
