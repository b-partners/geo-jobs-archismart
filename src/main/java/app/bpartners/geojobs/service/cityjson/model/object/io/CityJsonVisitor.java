package app.bpartners.geojobs.service.cityjson.model.object.io;

import app.bpartners.geojobs.service.cityjson.model.object.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CityJsonVisitor {

  /** Contexte fourni à chaque visiteur de surface. */
  public static final class SurfaceContext {
    public final CityJSONFeature feature;
    public final CityObject cityObject;
    public final Geometry geometry;
    public final int surfaceIndex;
    public final Map<String, Object> surface;
    public final VertexResolver vertexResolver;

    SurfaceContext(
        CityJSONFeature feature,
        CityObject co,
        Geometry geom,
        int surfaceIndex,
        Map<String, Object> surface,
        VertexResolver resolver) {
      this.feature = feature;
      this.cityObject = co;
      this.geometry = geom;
      this.surfaceIndex = surfaceIndex;
      this.surface = surface;
      this.vertexResolver = resolver;
    }
  }

  /** Itère sur chaque surface sémantique de tous les features du document. */
  public static void forEachSurface(CityJsonIO.Doc doc, Consumer<SurfaceContext> action) {
    Transform transform = doc.header.getTransform();

    for (CityJSONFeature feature : doc.features) {
      if (feature.getCityObjects() == null) continue;
      VertexResolver resolver = new VertexResolver(feature.getVertices(), transform);

      for (CityObject co : feature.getCityObjects().values()) {
        if (co.getGeometry() == null) continue;

        for (Geometry geom : co.getGeometry()) {
          if (geom.getSemantics() == null) continue;
          List<Map<String, Object>> surfaces = geom.getSemantics().getSurfaces();
          if (surfaces == null) continue;

          for (int i = 0; i < surfaces.size(); i++) {
            action.accept(new SurfaceContext(feature, co, geom, i, surfaces.get(i), resolver));
          }
        }
      }
    }
  }
}
