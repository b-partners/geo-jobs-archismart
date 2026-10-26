package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import app.bpartners.geojobs.endpoint.rest.model.AreaCoverage;
import app.bpartners.geojobs.endpoint.rest.model.CoverageImageryLayer;
import app.bpartners.geojobs.endpoint.rest.model.CoverageType;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.GeoPosition;
import app.bpartners.geojobs.endpoint.rest.model.ImageryCoverage;
import app.bpartners.geojobs.endpoint.rest.model.LidarCoverage;
import app.bpartners.geojobs.service.coverage.Coverage2D;
import app.bpartners.geojobs.service.coverage.Coverage3D;
import app.bpartners.geojobs.service.coverage.CoverageLocation;
import app.bpartners.geojobs.service.coverage.ImageryLayer;
import app.bpartners.geojobs.service.coverage.LocationCoverage;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CoverageRestMapper {
  public Set<app.bpartners.geojobs.service.coverage.CoverageType> toDomain(
      List<CoverageType> types) {
    var domainTypes = EnumSet.noneOf(app.bpartners.geojobs.service.coverage.CoverageType.class);
    if (types != null) {
      types.forEach(type -> domainTypes.add(toDomain(type)));
    }
    return domainTypes;
  }

  public AreaCoverage toRest(LocationCoverage coverage) {
    return new AreaCoverage()
        .position(toRest(coverage.location()))
        .imagery(coverage.imagery() == null ? null : toRest(coverage.imagery()))
        .lidar(coverage.lidar() == null ? null : toRest(coverage.lidar()));
  }

  private static app.bpartners.geojobs.service.coverage.CoverageType toDomain(CoverageType type) {
    return app.bpartners.geojobs.service.coverage.CoverageType.valueOf(type.name());
  }

  private static GeoPosition toRest(CoverageLocation location) {
    return new GeoPosition()
        .longitude(BigDecimal.valueOf(location.longitude()))
        .latitude(BigDecimal.valueOf(location.latitude()));
  }

  private static ImageryCoverage toRest(Coverage2D imagery) {
    return new ImageryCoverage()
        .covered(imagery.covered())
        .hd(imagery.hd())
        .layer(imagery.layer() == null ? null : toRest(imagery.layer()))
        .supportedDetectableModels(
            imagery.supportedModels().stream()
                .map(modelName -> new DetectableObjectModel().modelName(modelName))
                .toList());
  }

  private static CoverageImageryLayer toRest(ImageryLayer layer) {
    return new CoverageImageryLayer()
        .name(layer.name())
        .year(layer.year())
        .source(layer.source())
        .precisionInCm(layer.precisionInCm());
  }

  private static LidarCoverage toRest(Coverage3D lidar) {
    return new LidarCoverage()
        .covered(lidar.covered())
        .region(lidar.region())
        .crs(lidar.crs())
        .fileCount(lidar.fileCount());
  }
}
