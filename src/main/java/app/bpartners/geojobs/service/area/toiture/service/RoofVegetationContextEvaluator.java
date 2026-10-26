package app.bpartners.geojobs.service.area.toiture.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE_INDIVIDUALISE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.CANOPE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_ARBORE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_VERT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_VERT_PARKING;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.MultiPolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.area.toiture.model.CoveringType;
import app.bpartners.geojobs.service.area.toiture.model.RoofContext;
import app.bpartners.geojobs.service.area.toiture.model.RoofType;
import app.bpartners.geojobs.service.area.toiture.model.VegetationContext;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.UnaryUnionOp;

@RequiredArgsConstructor
public class RoofVegetationContextEvaluator {
  private static final double VEGETATION_RADIUS_METERS = 30.0;

  private static final double NO_VEGETATION_DISTANCE_METERS = 1000.0;

  private static final Set<DetectableType> VEGETATION_TYPES =
      EnumSet.of(
          ARBRE, ARBRE_INDIVIDUALISE, CANOPE, ESPACE_ARBORE, ESPACE_VERT, ESPACE_VERT_PARKING);

  private static final GeometrySquareMeterArea GEOMETRY_SQUARE_METER_AREA =
      new GeometrySquareMeterArea();

  private final Geometry roofGeometry;

  private final double roofArea;

  private final Collection<MultiPolygonObjectType> multiPolygonObjectTypes;

  private final Double roofSlopeInDegrees;

  private final CoveringType coveringType;

  private final boolean hasDrainageSystem;

  public double computeArea(DetectableType detectableType) {
    if (roofArea <= 0) {
      throw new BadRequestException(
          "Roof area cannot be zero or negative, current value " + roofArea);
    }
    return multiPolygonObjectTypes.stream()
        .filter(o -> detectableType.equals(o.objectType()))
        .map(MultiPolygonObjectType::multiPolygon)
        .map(RoofVegetationContextEvaluator::projectToMeters)
        .mapToDouble(Geometry::getArea)
        .sum();
  }

  /**
   * True only when vegetation-type detections were actually found near the roof. Vegetation
   * detection is not requested for every job today (e.g. the TOITURE model alone does not request
   * it), so an empty result here can mean "no vegetation nearby" or just as easily "not looked for"
   * - callers must not treat the absence of vegetation data as a confirmed negative.
   */
  public boolean hasVegetationData() {
    return !vegetationPolygonsInMeters().isEmpty();
  }

  public VegetationContext getVegetationContext() {
    var vegetationInMeters = vegetationPolygonsInMeters();
    if (vegetationInMeters.isEmpty()) {
      return new VegetationContext(NO_VEGETATION_DISTANCE_METERS, 0.0);
    }
    var roofInMeters = projectToMeters(roofGeometry);
    var vegetationUnion = UnaryUnionOp.union(vegetationInMeters);
    var distanceMinMeters = roofInMeters.distance(vegetationUnion);
    var surfVeg30mSqMeters =
        vegetationUnion.intersection(roofInMeters.buffer(VEGETATION_RADIUS_METERS)).getArea();
    return new VegetationContext(distanceMinMeters, surfVeg30mSqMeters);
  }

  public RoofContext getRoofContext() {
    return new RoofContext(
        classifyRoofType(roofSlopeInDegrees),
        // When the covering is unknown, be conservative about fire risk (never lower it)
        coveringType == null ? CoveringType.HIGH_COMBUSTIBILITY : coveringType,
        hasDrainageSystem);
  }

  private List<Geometry> vegetationPolygonsInMeters() {
    return multiPolygonObjectTypes.stream()
        .filter(o -> VEGETATION_TYPES.contains(o.objectType()))
        .map(MultiPolygonObjectType::multiPolygon)
        .map(RoofVegetationContextEvaluator::projectToMeters)
        .toList();
  }

  private static Geometry projectToMeters(Geometry geometry) {
    return GEOMETRY_SQUARE_METER_AREA.project(geometry, WGS84, LAMBERT_93);
  }

  private static RoofType classifyRoofType(Double slopeInDegrees) {
    if (slopeInDegrees == null) {
      // Unknown slope is treated as "neutral" for the fire risk computation
      return RoofType.MEDIUM_SLOPE;
    }
    if (slopeInDegrees <= 5) {
      return RoofType.FLAT_OR_VERY_LOW_SLOPE;
    }
    if (slopeInDegrees <= 15) {
      return RoofType.LOW_SLOPE;
    }
    if (slopeInDegrees <= 45) {
      return RoofType.MEDIUM_SLOPE;
    }
    return RoofType.STEEP_SLOPE;
  }
}
