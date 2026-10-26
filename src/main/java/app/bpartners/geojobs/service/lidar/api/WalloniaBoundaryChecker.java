package app.bpartners.geojobs.service.lidar.api;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker.parseGeoJsonPolygon;

import app.bpartners.geojobs.model.exception.ApiException;
import java.nio.charset.StandardCharsets;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class WalloniaBoundaryChecker {
  private final Geometry walloniaBoundary;

  public WalloniaBoundaryChecker() {
    try {
      ClassPathResource resource = new ClassPathResource("files/wallonia_boundary.geojson");
      String geoJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      this.walloniaBoundary = parseGeoJsonPolygon(geoJson);
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public boolean isGeometryInWallonia(Geometry geometry) {
    Geometry fixed = GeometryFixer.fix(geometry);
    return walloniaBoundary.getEnvelopeInternal().intersects(fixed.getEnvelopeInternal())
        && walloniaBoundary.contains(fixed);
  }
}
