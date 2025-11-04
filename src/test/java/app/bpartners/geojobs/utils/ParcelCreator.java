package app.bpartners.geojobs.utils;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.SneakyThrows;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class ParcelCreator {

  ObjectMapper om = new ObjectMapper();
  TileCreator tileCreator = new TileCreator();

  public Parcel create(int nbTilePerParcel) {
    if (nbTilePerParcel < 0)
      throw new RuntimeException("nbTilePerParcel must be > 0 but was " + nbTilePerParcel);
    var tiles = new ArrayList<Tile>();
    for (int j = 0; j < nbTilePerParcel; j++) {
      tiles.add(tileCreator.create(randomUUID().toString(), "bucketPath-" + randomUUID()));
    }
    return create(randomUUID().toString(), tiles);
  }

  private Parcel create(String id, Tile tile) {
    return create(id, List.of(tile));
  }

  @SneakyThrows
  public Parcel create(String id, List<Tile> tiles) {
    HashMap<String, Object> properties = new HashMap<>();
    var featureId = randomUUID().toString();
    int zoom = 20;
    properties.put("id", featureId);
    properties.put("zoom", zoom);
    return Parcel.builder()
        .id(id)
        .parcelContent(
            ParcelContent.builder()
                .geoServerUrl(new URI("https://dummy.com").toURL())
                .feature(
                    Feature.builder()
                        .id(featureId)
                        .zoom(zoom)
                        .geometry(
                            Feature.FeatureGeometry.builder()
                                .actualInstanceStringValue(
                                    objectMapper()
                                        .writeValueAsString(
                                            new MultiPolygon()
                                                .type(POLYGON)
                                                .coordinates(
                                                    List.of(
                                                        List.of(
                                                            List.of(
                                                                List.of(
                                                                    new BigDecimal("0.0"),
                                                                    new BigDecimal("0.0"))))))))
                                .build())
                        .properties(properties)
                        .build())
                .geoServerParameter(
                    om.readValue(
                        "{\n"
                            + "    \"service\": \"WMS\",\n"
                            + "    \"request\": \"GetMap\",\n"
                            + "    \"layers\": \"grandlyon:ortho_2018\",\n"
                            + "    \"styles\": \"\",\n"
                            + "    \"format\": \"image/png\",\n"
                            + "    \"transparent\": true,\n"
                            + "    \"version\": \"1.3.0\",\n"
                            + "    \"width\": 256,\n"
                            + "    \"height\": 256,\n"
                            + "    \"srs\": \"EPSG:3857\"\n"
                            + "  }",
                        GeoServerParameter.class))
                .tiles(tiles)
                .build())
        .build();
  }

  private String mockFeature() {
    return "{ \"type\": \"Feature\",\n"
        + "  \"properties\": {\n"
        + "    \"code\": \"69\",\n"
        + "    \"nom\": \"Rh\u00f4ne\",\n"
        + "    \"id\": 30251921,\n"
        + "    \"CLUSTER_ID\": 99520,\n"
        + "    \"CLUSTER_SIZE\": 386884 },\n"
        + "  \"geometry\": {\n"
        + "    \"type\": \"MultiPolygon\",\n"
        + "    \"coordinates\": [ [ [\n"
        + "      [ 4.459648282829194, 45.904988912620688 ],\n"
        + "      [ 4.464709510872551, 45.928950368349426 ],\n"
        + "      [ 4.490816965688656, 45.941784543770964 ],\n"
        + "      [ 4.510354299995861, 45.933697132664598 ],\n"
        + "      [ 4.518386257467152, 45.912888345521047 ],\n"
        + "      [ 4.496344031095243, 45.883438201401809 ],\n"
        + "      [ 4.479593950305621, 45.882900828315755 ],\n"
        + "      [ 4.459648282829194, 45.904988912620688 ] ] ] ] }";
  }
}
