package app.bpartners.geojobs.service.cityjson.model.object.io;

import app.bpartners.geojobs.service.cityjson.model.object.Geometry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.Consumer;

public final class SurfaceAnnotators {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Ajoute "slope_in_degrees" sur les surfaces qui ont "rf_slope". */
  public static Consumer<CityJsonVisitor.SurfaceContext> slopeInDegrees() {
    return ctx -> {
      Object slope = ctx.surface.get("rf_slope");
      if (slope instanceof Number n) {
        double deg = Math.toDegrees(Math.atan(n.doubleValue()));
        ctx.surface.put("slope_in_degrees", n);
      }
    };
  }

  /** Ajoute "height_in_meters" sur les WallSurface : extension verticale (Zmax - Zmin). */
  public static Consumer<CityJsonVisitor.SurfaceContext> wallHeightInMeters() {
    return onlyOn(
        "WallSurface",
        ctx -> {
          double height = computeHeightForSurface(ctx);
          if (height > 0) ctx.surface.put("height_in_meters", round(height, 3));
        });
  }

  private static double computeHeightForSurface(CityJsonVisitor.SurfaceContext ctx) {
    Geometry geom = ctx.geometry;
    if (!"Solid".equals(geom.getType()) && !"MultiSurface".equals(geom.getType())) {
      return 0.0;
    }
    boolean isSolid = "Solid".equals(geom.getType());

    Object boundariesRaw = geom.getBoundaries();
    Object valuesRaw = geom.getSemantics().getValues();

    double zMin = Double.POSITIVE_INFINITY;
    double zMax = Double.NEGATIVE_INFINITY;

    if (isSolid) {
      List<List<List<List<Integer>>>> shells =
          MAPPER.convertValue(boundariesRaw, new TypeReference<>() {});
      List<List<Integer>> values = MAPPER.convertValue(valuesRaw, new TypeReference<>() {});
      for (int s = 0; s < shells.size(); s++) {
        double[] zRange =
            zRangeForShell(shells.get(s), values.get(s), ctx.surfaceIndex, ctx.vertexResolver);
        zMin = Math.min(zMin, zRange[0]);
        zMax = Math.max(zMax, zRange[1]);
      }
    } else { // MultiSurface
      List<List<List<Integer>>> polygons =
          MAPPER.convertValue(boundariesRaw, new TypeReference<>() {});
      List<Integer> values = MAPPER.convertValue(valuesRaw, new TypeReference<>() {});
      double[] zRange = zRangeForShell(polygons, values, ctx.surfaceIndex, ctx.vertexResolver);
      zMin = zRange[0];
      zMax = zRange[1];
    }

    if (zMin == Double.POSITIVE_INFINITY) return 0.0;
    return zMax - zMin;
  }

  private static double[] zRangeForShell(
      List<List<List<Integer>>> polygons,
      List<Integer> polygonValues,
      int targetSurfaceIdx,
      VertexResolver resolver) {
    double zMin = Double.POSITIVE_INFINITY;
    double zMax = Double.NEGATIVE_INFINITY;

    for (int p = 0; p < polygons.size(); p++) {
      Integer surfIdx = polygonValues.get(p);
      if (surfIdx == null || surfIdx != targetSurfaceIdx) continue;

      for (List<Integer> ring : polygons.get(p)) {
        for (Integer idx : ring) {
          double z = resolver.get(idx)[2];
          if (z < zMin) zMin = z;
          if (z > zMax) zMax = z;
        }
      }
    }
    return new double[] {zMin, zMax};
  }

  /** Ajoute "rf_area_m2" : aire 3D réelle (pente comprise) du polygone. */
  public static Consumer<CityJsonVisitor.SurfaceContext> areaM2() {
    return ctx -> {
      double area = computeAreaForSurface(ctx);
      if (area > 0) ctx.surface.put("area_in_square_meters", round(area, 3));
    };
  }

  /** Filtre : applique l'annotateur seulement sur certains types de surface. */
  public static Consumer<CityJsonVisitor.SurfaceContext> onlyOn(
      String surfaceType, Consumer<CityJsonVisitor.SurfaceContext> inner) {
    return ctx -> {
      if (surfaceType.equals(ctx.surface.get("type"))) inner.accept(ctx);
    };
  }

  // -------- internals --------

  private static double computeAreaForSurface(CityJsonVisitor.SurfaceContext ctx) {
    Geometry geom = ctx.geometry;
    if (!"Solid".equals(geom.getType()) && !"MultiSurface".equals(geom.getType())) {
      return 0.0;
    }

    // Profondeur de boundaries : Solid = 4, MultiSurface = 3
    // Profondeur de values     : Solid = 2, MultiSurface = 1
    boolean isSolid = "Solid".equals(geom.getType());

    Object boundariesRaw = geom.getBoundaries();
    Object valuesRaw = geom.getSemantics().getValues();

    double total = 0.0;

    if (isSolid) {
      List<List<List<List<Integer>>>> shells =
          MAPPER.convertValue(boundariesRaw, new TypeReference<>() {});
      List<List<Integer>> values = MAPPER.convertValue(valuesRaw, new TypeReference<>() {});
      for (int s = 0; s < shells.size(); s++) {
        total +=
            areaForShell(shells.get(s), values.get(s), ctx.surfaceIndex, ctx.vertexResolver, false);
      }
    } else { // MultiSurface
      List<List<List<Integer>>> polygons =
          MAPPER.convertValue(boundariesRaw, new TypeReference<>() {});
      List<Integer> values = MAPPER.convertValue(valuesRaw, new TypeReference<>() {});
      total += areaForShell(polygons, values, ctx.surfaceIndex, ctx.vertexResolver, false);
    }
    return total;
  }

  private static double areaForShell(
      List<List<List<Integer>>> polygons,
      List<Integer> polygonValues,
      int targetSurfaceIdx,
      VertexResolver resolver,
      boolean projected) {
    double sum = 0.0;
    for (int p = 0; p < polygons.size(); p++) {
      Integer surfIdx = polygonValues.get(p);
      if (surfIdx == null || surfIdx != targetSurfaceIdx) continue;

      List<List<double[]>> rings = new ArrayList<>();
      for (List<Integer> ring : polygons.get(p)) {
        List<double[]> realRing = new ArrayList<>(ring.size());
        for (Integer idx : ring) realRing.add(resolver.get(idx));
        rings.add(realRing);
      }
      sum += projected ? PolygonArea.polygonAreaProjected(rings) : PolygonArea.polygonArea(rings);
    }
    return sum;
  }

  private static double round(double v, int decimals) {
    double f = Math.pow(10, decimals);
    return Math.round(v * f) / f;
  }
}
