package app.bpartners.geojobs.model.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.BATIMENT;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.fromValue;

import app.bpartners.geojobs.service.lidar.model.LidarClass;
import com.github.mreutegg.laszip4j.LASHeader;
import com.github.mreutegg.laszip4j.LASPoint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

@Getter
@EqualsAndHashCode(callSuper = true)
public class LasPointGeometry extends Point {
  @EqualsAndHashCode.Include private final LidarClass classification;

  public LasPointGeometry(LASPoint lasPoint, LASHeader lasHeader) {
    super(lasPointToSequence(lasPoint, lasHeader), geometryFactory);
    this.classification = fromValue(lasPoint.getClassification());
  }

  private static CoordinateSequence lasPointToSequence(LASPoint point, LASHeader header) {
    double x = point.getX() * header.getXScaleFactor() + header.getXOffset();
    double y = point.getY() * header.getYScaleFactor() + header.getYOffset();
    double z = point.getZ() * header.getZScaleFactor() + header.getZOffset();

    return new CoordinateArraySequence(new Coordinate[] {new Coordinate(x, y, z)});
  }

  public LasPointGeometry(Coordinate coordinate) {
    this(coordinate.getX(), coordinate.getY(), coordinate.getZ(), BATIMENT);
  }

  public LasPointGeometry(double x, double y, double z) {
    this(x, y, z, BATIMENT);
  }

  public LasPointGeometry(double x, double y, double z, LidarClass classification) {
    super(new CoordinateArraySequence(new Coordinate[] {new Coordinate(x, y, z)}), geometryFactory);
    this.classification = classification;
  }

  public double getZ() {
    return this.getCoordinate().getZ();
  }

  public double distance(LasPointGeometry other) {
    return Math.sqrt(squaredDistance(other));
  }

  public double squaredDistance(LasPointGeometry other) {
    double dx = this.getX() - other.getX();
    double dy = this.getY() - other.getY();
    double dz = this.getZ() - other.getZ();
    return dx * dx + dy * dy + dz * dz;
  }
}
