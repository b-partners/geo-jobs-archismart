package app.bpartners.geojobs.service.cityjson.model.object.io;

import app.bpartners.geojobs.service.cityjson.model.object.CityJSONFeature;
import app.bpartners.geojobs.service.cityjson.model.object.CityObject;
import app.bpartners.geojobs.service.cityjson.model.object.Geometry;
import app.bpartners.geojobs.service.cityjson.model.object.Transform;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public final class RoofAreaCalculator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Calcule l'aire de chaque surface sémantique pour un feature. Le résultat est ajouté comme
   * attribut "rf_area_m2" sur chaque entrée de semantics.surfaces (toutes, pas que les RoofSurface
   * — facile à filtrer).
   */
  public static void annotateSurfaceAreas(CityJSONFeature feature, Transform transform) {
    VertexResolver resolver = new VertexResolver(feature.getVertices(), transform);

    for (CityObject co : feature.getCityObjects().values()) {
      if (co.getGeometry() == null) continue;

      for (Geometry geom : co.getGeometry()) {
        if (geom.getSemantics() == null) continue;
        if (!"Solid".equals(geom.getType())) continue; // adapter si besoin

        // boundaries: List<List<List<List<Integer>>>>  (shells > polygons > rings > indices)
        List<List<List<List<Integer>>>> boundaries =
            MAPPER.convertValue(
                geom.getBoundaries(), new TypeReference<List<List<List<List<Integer>>>>>() {});
        // values: List<List<Integer>> (shells > polygons)
        List<List<Integer>> values =
            MAPPER.convertValue(
                geom.getSemantics().getValues(), new TypeReference<List<List<Integer>>>() {});

        // accumule l'aire par index de surface sémantique
        Map<Integer, Double> areaBySurface = new HashMap<>();

        for (int s = 0; s < boundaries.size(); s++) {
          List<List<List<Integer>>> shell = boundaries.get(s);
          List<Integer> shellValues = values.get(s);

          for (int p = 0; p < shell.size(); p++) {
            Integer surfIdx = shellValues.get(p);
            if (surfIdx == null) continue; // null = pas de sémantique

            // construit les anneaux (extérieur + trous) en coords réelles
            List<List<double[]>> rings = new ArrayList<>();
            for (List<Integer> ring : shell.get(p)) {
              List<double[]> realRing = new ArrayList<>(ring.size());
              for (Integer idx : ring) realRing.add(resolver.get(idx));
              rings.add(realRing);
            }

            double area = PolygonArea.polygonArea(rings);
            areaBySurface.merge(surfIdx, area, Double::sum);
          }
        }

        // écrit le résultat dans semantics.surfaces[i]
        List<Map<String, Object>> surfaces = geom.getSemantics().getSurfaces();
        for (Map.Entry<Integer, Double> e : areaBySurface.entrySet()) {
          Map<String, Object> surf = surfaces.get(e.getKey());
          // arrondi à 3 décimales pour ne pas polluer le JSON
          double rounded = Math.round(e.getValue() * 1000.0) / 1000.0;
          surf.put("area_in_square_meters", rounded);
        }
      }
    }
  }
}
