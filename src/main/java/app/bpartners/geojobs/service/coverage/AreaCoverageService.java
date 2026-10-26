package app.bpartners.geojobs.service.coverage;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.GatewayTimeoutException;
import app.bpartners.geojobs.service.geodata.MapLayerResolver;
import app.bpartners.geojobs.service.geodata.model.GeodataMapLayer;
import app.bpartners.geojobs.service.lidar.api.GeodataLidarApiClient;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AreaCoverageService {
  static final double POINT_HALF_SIDE_IN_DEGREES = 0.00005;

  private final MapLayerResolver mapLayerResolver;
  private final GeodataLidarApiClient lidarApiClient;
  private final LidarFileChecker lidarFileChecker;
  private final CoverageConf conf;
  private final SupportedModelResolver supportedModelResolver;

  public LocationCoverage check(CoverageLocation location, Set<CoverageType> types) {
    var all = types == null || types.isEmpty();
    var imagery =
        all || types.contains(CoverageType.IMAGERY)
            ? check2D(location.longitude(), location.latitude())
            : null;
    var lidar =
        all || types.contains(CoverageType.LIDAR)
            ? check3D(location.longitude(), location.latitude())
            : null;
    return new LocationCoverage(location, imagery, lidar);
  }

  public Coverage2D check2D(double longitude, double latitude) {
    return mapLayerResolver
        .apply(longitude, latitude)
        .map(this::toCoverage2D)
        .orElseGet(Coverage2D::notCovered);
  }

  public Coverage3D check3D(double longitude, double latitude) {
    return check3D(List.of(pointFootprint(longitude, latitude)));
  }

  public Coverage3D check3D(Collection<Geometry> wgs84Geometries) {
    if (wgs84Geometries == null || wgs84Geometries.isEmpty()) {
      throw new BadRequestException("At least one geometry is required to check 3D coverage");
    }
    var geometries = List.copyOf(wgs84Geometries);
    var result = lidarApiClient.getLidarFileUrls(geometries);

    Set<String> distinctUrls = new HashSet<>();
    Map<String, LidarFileStatus> statusPerUrl = new HashMap<>();
    var coveredGeometryCount = 0;
    for (int i = 0; i < geometries.size(); i++) {
      var urls =
          i < result.urlsPerGeometry().size() ? result.urlsPerGeometry().get(i) : Set.<String>of();
      distinctUrls.addAll(urls);
      if (hasValidFile(urls, statusPerUrl)) {
        coveredGeometryCount++;
      }
    }
    return new Coverage3D(
        coveredGeometryCount == geometries.size(),
        result.region(),
        result.crs(),
        distinctUrls.size(),
        coveredGeometryCount,
        geometries.size());
  }

  private boolean hasValidFile(Set<String> urls, Map<String, LidarFileStatus> statusPerUrl) {
    String unverifiedUrl = null;
    for (var url : urls) {
      var status = statusPerUrl.computeIfAbsent(url, lidarFileChecker::check);
      if (status == LidarFileStatus.VALID) {
        return true;
      }
      if (status == LidarFileStatus.UNKNOWN) {
        unverifiedUrl = url;
      }
    }
    if (unverifiedUrl != null) {
      throw new GatewayTimeoutException(
          "Lidar file " + unverifiedUrl + " could not be verified, 3D coverage is undetermined");
    }
    return false;
  }

  private static Geometry pointFootprint(double longitude, double latitude) {
    var d = POINT_HALF_SIDE_IN_DEGREES;
    return geometryFactory.createPolygon(
        new Coordinate[] {
          new Coordinate(longitude - d, latitude - d),
          new Coordinate(longitude + d, latitude - d),
          new Coordinate(longitude + d, latitude + d),
          new Coordinate(longitude - d, latitude + d),
          new Coordinate(longitude - d, latitude - d)
        });
  }

  private Coverage2D toCoverage2D(GeodataMapLayer layer) {
    if (layer == null) {
      return Coverage2D.notCovered();
    }
    var precision = layer.precisionLevelInCm();
    var hd = precision != null && precision <= conf.getHdMaxPrecisionInCm();
    return new Coverage2D(
        true,
        hd,
        new ImageryLayer(layer.name(), layer.year(), layer.source(), precision),
        supportedModelResolver.supportedModels(precision));
  }
}
