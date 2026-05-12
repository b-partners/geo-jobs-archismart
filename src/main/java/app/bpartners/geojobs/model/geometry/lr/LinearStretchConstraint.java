package app.bpartners.geojobs.model.geometry.lr;

import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector2D;

@Getter
public class LinearStretchConstraint {
  private final double degTolerance;
  private final double radTolerance;

  private final Vector2D direction;
  private final Coordinate stretchPoint;

  public LinearStretchConstraint(
      Coordinate stretchPoint, Coordinate baseNeighbor, double degTolerance) {
    this.degTolerance = degTolerance;
    this.radTolerance = Math.toRadians(degTolerance);

    this.stretchPoint = stretchPoint;
    this.direction = new Vector2D(stretchPoint, baseNeighbor).normalize();
  }

  public boolean isAligned(Coordinate candidate) {
    if (stretchPoint == candidate) return true;

    var candidateDirection = new Vector2D(stretchPoint, candidate).normalize();
    var dot = direction.dot(candidateDirection);
    var securedDot = Math.clamp(dot, -1.0, 1.0);
    var angle = Math.acos(securedDot);
    return angle <= radTolerance;
  }
}
