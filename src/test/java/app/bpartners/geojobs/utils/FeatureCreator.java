package app.bpartners.geojobs.utils;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;

public class FeatureCreator {

  @NonNull
  @SneakyThrows
  public List<Feature> defaultFeatures() {
    return List.of(
        new ObjectMapper()
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(
                "{ \"type\": \"Feature\",\n"
                    + "  \"properties\": {\n"
                    + "    \"code\": \"69\",\n"
                    + "    \"nom\": \"Rh\u00f4ne\",\n"
                    + "    \"id\": \"feature1_id\",\n"
                    + "    \"zoom\": 20,\n"
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
                    + "      [ 4.459648282829194, 45.904988912620688 ] ] ] ] } }",
                Feature.class));
  }
}
