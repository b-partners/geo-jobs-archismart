package app.bpartners.geojobs.model.geometry.area.rate;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_CLAIR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_INTENSE;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.model.geometry.MultiPolygonObjectType;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.service.PolygonObjectTypeConverter;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.Collection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class HumiditeAreaRateComputer extends AreaRateComputer {
  private final FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(), null);
  private final double roofArea;
  private final DetectedTile tile;
  private final Collection<MultiPolygonObjectType> multiPolygonObjectTypes;

  public HumiditeAreaRateComputer(double roofArea, DetectedTile tile) {
    this.roofArea = roofArea;
    this.tile = tile;
    this.multiPolygonObjectTypes = null;
  }

  public HumiditeAreaRateComputer(
      double roofArea, Collection<PolygonObjectType> polygonObjectTypes) {
    this.tile = null;
    this.roofArea = roofArea;
    this.multiPolygonObjectTypes = new PolygonObjectTypeConverter().convertFrom(polygonObjectTypes);
  }

  @Override
  public double compute(DetectableType detectableType) {
    if (roofArea <= 0) {
      throw new BadRequestException(
          "Roof area cannot be zero or negative, current value" + roofArea);
    }
    if (tile == null && multiPolygonObjectTypes != null) {
      double computedArea =
          multiPolygonObjectTypes.stream()
              .filter(o -> detectableType.equals(o.objectType()))
              .map(MultiPolygonObjectType::multiPolygon)
              .mapToDouble(MultiPolygon::getArea)
              .sum();
      return (getMalus(detectableType) * computedArea) / roofArea;
    } else if (tile != null) {
      double computedArea =
          tile.getDetectedObjects().stream()
              .filter(o -> o.getDetectableObjectType().equals(detectableType))
              .map(DetectedObject::getFeature)
              .map(featureMapper::toDomainPolygon)
              .mapToDouble(Polygon::getArea)
              .sum();
      return (getMalus(detectableType) * computedArea) / roofArea;
    }
    throw new IllegalStateException(
        "Both tile and polygonObjectTypes can not be null to compute HumiditeAreaRate");
  }

  private int getMalus(DetectableType detectableType) {
    return switch (detectableType) {
      case HUMIDITE_CLAIR -> 1;
      case HUMIDITE_INTENSE -> 2;
      default ->
          throw new NotImplementedException(
              "Detectable type " + detectableType + " malus not implemented");
    };
  }

  public double getHumidityAreaRate() {
    var computedAreaRate = (compute(HUMIDITE_CLAIR) + compute(HUMIDITE_INTENSE)) * 100;
    return Math.min(computedAreaRate, 100.0);
  }
}
