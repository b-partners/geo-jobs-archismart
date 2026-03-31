package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities.project;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.PointsDelimitationComputer.getConvex;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.Polygon3DArea;
import app.bpartners.geojobs.model.lidar.planes.PlaneDelimitation.PlaneDelimitationConf;
import app.bpartners.geojobs.model.lidar.planes.algorithm.GeometryUtilities;
import app.bpartners.geojobs.model.lidar.planes.algorithm.Vector3DUtils;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.math.Vector3D;

@Slf4j
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Plane3D {
  @EqualsAndHashCode.Include protected final double a;
  @EqualsAndHashCode.Include protected final double b;
  @EqualsAndHashCode.Include protected final double c;
  @EqualsAndHashCode.Include protected final double d;
  @EqualsAndHashCode.Exclude protected final Kernel kernel;
  @EqualsAndHashCode.Exclude protected final Set<LasPointGeometry> points;
  @EqualsAndHashCode.Exclude protected final PlaneDelimitationConf delimitationConf;

  @EqualsAndHashCode.Include private Double norm;
  @EqualsAndHashCode.Exclude private Polygon3DArea area;
  @EqualsAndHashCode.Exclude protected Polygon delimitation;
  @EqualsAndHashCode.Exclude private LasPointGeometry centroid;
  @EqualsAndHashCode.Exclude protected Polygon convexDelimitation;
  @EqualsAndHashCode.Exclude private Plane3DSlopeInDegrees slopeInDegrees;
  @EqualsAndHashCode.Exclude protected final Plane3DExtractionStepExporter exporter;

  public double distance(LasPointGeometry p) {
    return Math.abs(a * p.getX() + b * p.getY() + c * p.getZ() + d) / getNorm();
  }

  public Plane3D with(Set<LasPointGeometry> points) {
    return this.toBuilder().points(new HashSet<>(points)).build();
  }

  public static Plane3D empty() {
    return Plane3D.builder()
        .a(0)
        .b(0)
        .c(0)
        .d(0)
        .norm(1d)
        .exporter(null)
        .points(Set.of())
        .delimitationConf(null)
        .build();
  }

  public Plane3DSlopeInDegrees getSlopeInDegrees() {
    if (slopeInDegrees == null) {
      slopeInDegrees = new Plane3DSlopeInDegrees(points);
    }

    return slopeInDegrees;
  }

  public Polygon getDelimitation() {
    if (delimitation == null) {
      var delimiter = new PlaneDelimitation(delimitationConf, points, exporter);
      delimitation = delimiter.getPolygon();
      delimitation = project(this, delimitation);
    }

    return delimitation;
  }

  public double get2DArea() {
    return getDelimitation().getArea();
  }

  public double getNorm() {
    if (norm == null) {
      norm = Math.sqrt(a * a + b * b + c * c);
    }
    return norm;
  }

  public double getArea() {
    if (area == null) {
      area = new Polygon3DArea(getDelimitation());
    }

    return area.getValue();
  }

  public Polygon getConvexDelimitation() {
    if (convexDelimitation == null) {
      var convex = getConvex(points);
      convexDelimitation = project(this, convex);
    }
    return convexDelimitation;
  }

  public boolean isVertical() {
    return Math.abs(c) < 1e-12;
  }

  public double zAt(double x, double y) {
    if (isVertical()) {
      throw new IllegalStateException("Plane is vertical: z cannot be computed for given x,y");
    }
    return -(a * x + b * y + d) / c;
  }

  private Vector3D normal;

  private Vector3D getNormal() {
    if (normal == null) {
      normal = new Vector3D(a, b, c).normalize();
    }
    return normal;
  }

  private Vector3D axisU;

  private Vector3D getAxisU() {
    if (axisU == null) {
      axisU = Vector3DUtils.cross(getNormal(), new Vector3D(0, 0, 1));
      if (axisU.length() < 1e-6) {
        axisU = Vector3DUtils.cross(getNormal(), new Vector3D(0, 1, 0));
      }
      axisU = axisU.normalize();
    }

    return axisU;
  }

  private Vector3D axisV;

  private Vector3D getAxisV() {
    if (axisV == null) {
      axisV = Vector3DUtils.cross(getNormal(), getAxisU()).normalize();
    }
    return axisV;
  }

  public Vector2D projectToLocal(LasPointGeometry p) {
    var cen = this.getCentroid();
    var dx = p.getX() - cen.getX();
    var dy = p.getY() - cen.getY();
    var dz = p.getZ() - cen.getZ();
    var local = new Vector3D(dx, dy, dz);

    double u = local.dot(getAxisU());
    double v = local.dot(getAxisV());
    return new Vector2D(u, v);
  }

  public LasPointGeometry getCentroid() {
    if (centroid == null) {
      centroid = GeometryUtilities.centroid(points);
    }
    return centroid;
  }
}
