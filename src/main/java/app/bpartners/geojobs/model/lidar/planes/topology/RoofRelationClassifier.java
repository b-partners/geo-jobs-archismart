package app.bpartners.geojobs.model.lidar.planes.topology;

import static app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType.*;

import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.model.lidar.planes.topology.model.RoofRelationType;
import java.util.function.BiFunction;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.math.Vector2D;

@RequiredArgsConstructor
public class RoofRelationClassifier implements BiFunction<Plane3D, Plane3D, RoofRelationType> {
  private final RoofRelationClassifierConf conf;

  @Override
  public RoofRelationType apply(Plane3D a, Plane3D b) {
    var normA = Vector2D.create(a.getA(), a.getB());
    var normB = Vector2D.create(b.getA(), b.getB());
    if (normA.lengthSquared() < 1e-12 || normB.lengthSquared() < 1e-12) {
      return NONE;
    }

    var nA = normA.normalize();
    var nB = normB.normalize();

    var cosTheta = nA.dot(nB);
    var angleDeg = Math.toDegrees(Math.acos(cosTheta));

    var vAB =
        new Vector2D(
                b.getCentroid().getX() - a.getCentroid().getX(),
                b.getCentroid().getY() - a.getCentroid().getY())
            .normalize();
    var lookAtTest = nA.dot(vAB);

    if (Math.abs(angleDeg - 180.0) < conf.angleThresholdDeg()) {
      return lookAtTest < 0 ? S : NONE;
    }

    if (Math.abs(angleDeg - 90.0) < conf.angleThresholdDeg()) {
      if (lookAtTest > 0) {
        return O_MINUS;
      }
      return O_PLUS;
    }

    // TODO: add O_PLUS_ASYMMETRIC & O_MINUS_ASYMMETRIC
    return NONE;
  }

  @Builder
  public record RoofRelationClassifierConf(double angleThresholdDeg) {}
}
