package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.invert;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.BATI_BETON;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BoundaryMergerTest {
  PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/dijon.json");
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  TilingConf tilingConf = new TilingConf(20, 1_024);
  BoundaryMerger boundaryMerger = new BoundaryMerger(4000, 41, true);

  @Test
  void boundary_merger_apply() {
    var geojsonFile =
        new File(getClass().getResource("/geometry/geojson/bati_4_polygons.geojson").getFile());

    var polygons = geoJsonLoader.apply(geojsonFile);
    var inverted = invert(polygons);

    Set<TiledPolygon> tiledPolygons =
        inverted.stream().map(latLon -> latLon.tiledPolygon(tilingConf)).collect(toSet());

    Set<LatLonPolygon> unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    assertTrue(tiledPolygons.size() > unified.size());
    assertEquals(1, unified.size());
  }

  @Test
  void run() {
    var geojsonFile = new File(getClass().getResource("/ivandry/bati.geojson").getFile());

    var polygons = geoJsonLoader.apply(geojsonFile);
    var inverted = invert(polygons);

    var tiledPolygons =
        inverted.stream().map(latLon -> latLon.tiledPolygon(tilingConf)).collect(toSet());
    var unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    // new Geojson(unified).saveAsFile("bati_dijon.geojson");
  }

  @Test
  void run_from_vgg() {
    var tiledPolygons = polygonProvider.getTiledPolygons(false);

    var unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    // new Geojson(unified).saveAsFile("castanet-map-87-tree_initial.geojson");
  }

  @Test
  void run_from_annotations() {
    var annotations = polygonProvider.getVggAnnotations();

    var unified =
        boundaryMerger.apply(annotations).stream()
            .map(TiledPolygon::latLonPolygon)
            .collect(toSet());

    // new Geojson(unified).saveAsFile("test.geojson");
  }
}
