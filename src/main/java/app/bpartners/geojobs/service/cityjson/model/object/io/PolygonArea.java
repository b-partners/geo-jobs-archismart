package app.bpartners.geojobs.service.cityjson.model.object.io;

import java.util.List;

public final class PolygonArea {

  /** Aire d'un anneau 3D (liste de sommets, non refermée). */
  public static double ringArea(List<double[]> ring) {
    if (ring.size() < 3) return 0.0;
    double[] p0 = ring.get(0);
    double nx = 0, ny = 0, nz = 0;
    for (int i = 1; i < ring.size() - 1; i++) {
      double[] a = ring.get(i);
      double[] b = ring.get(i + 1);
      double ax = a[0] - p0[0], ay = a[1] - p0[1], az = a[2] - p0[2];
      double bx = b[0] - p0[0], by = b[1] - p0[1], bz = b[2] - p0[2];
      // produit vectoriel (a-p0) x (b-p0)
      nx += ay * bz - az * by;
      ny += az * bx - ax * bz;
      nz += ax * by - ay * bx;
    }
    return 0.5 * Math.sqrt(nx * nx + ny * ny + nz * nz);
  }

  /** Aire d'un polygone : 1er anneau = extérieur, suivants = trous. */
  public static double polygonArea(List<List<double[]>> rings) {
    if (rings.isEmpty()) return 0.0;
    double area = ringArea(rings.get(0));
    for (int i = 1; i < rings.size(); i++) {
      area -= ringArea(rings.get(i));
    }
    return area;
  }

  public static double ringAreaProjected(List<double[]> ring) {
    if (ring.size() < 3) return 0.0;
    double[] p0 = ring.get(0);
    double nz = 0;
    for (int i = 1; i < ring.size() - 1; i++) {
      double[] a = ring.get(i);
      double[] b = ring.get(i + 1);
      double ax = a[0] - p0[0], ay = a[1] - p0[1];
      double bx = b[0] - p0[0], by = b[1] - p0[1];
      nz += ax * by - ay * bx;
    }
    return 0.5 * Math.abs(nz);
  }

  public static double polygonAreaProjected(List<List<double[]>> rings) {
    if (rings.isEmpty()) return 0.0;
    double area = ringAreaProjected(rings.get(0));
    for (int i = 1; i < rings.size(); i++) {
      area -= ringAreaProjected(rings.get(i));
    }
    return area;
  }
}
