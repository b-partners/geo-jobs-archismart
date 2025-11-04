package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.toTiledPolygons;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.feature.Feature;
import app.bpartners.geojobs.model.geometry.feature.FeatureListWithOffset;
import app.bpartners.geojobs.model.geometry.feature.FeatureListWithoutOffset;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import org.locationtech.jts.geom.Polygon;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class PolygonProvider implements Function<Integer, Polygon> {

  private static final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

  private final IntXY origin;
  @Getter private final VGG vggAnnotations;
  private final List<Feature> features;

  public PolygonProvider(
      String vggFilePath, IntXY origin, IntXY imageResolution, boolean is_z_x_y_dot_filetype) {
    this.origin = origin;
    this.vggAnnotations = vgg(vggFilePath);
    this.features = features(imageResolution, is_z_x_y_dot_filetype);
  }

  private VGG vgg(String vggFilePath) {
    var resource = getClass().getResource(vggFilePath);
    try (var file = new RandomAccessFile(new File(resource.toURI()), "r")) {
      var channel = file.getChannel();
      var buffer = channel.map(READ_ONLY, 0, channel.size());
      var decoded = UTF_8.decode(buffer);
      var vggAsString = decoded.toString();
      return om.readValue(vggAsString, VGG.class);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public PolygonProvider(String vggFilePath, IntXY origin, IntXY imageResolution) {
    this.origin = origin;
    this.vggAnnotations = vgg(vggFilePath);
    this.features = features(imageResolution, false);
  }

  public PolygonProvider(String vggFilePath) {
    this.origin = null;
    this.vggAnnotations = vgg(vggFilePath);
    this.features = null;
  }

  @Override
  public Polygon apply(Integer n) {
    return features.get(n).geometry();
  }

  public int featuresNb() {
    return features.size();
  }

  private List<Feature> features(IntXY imageResolution, boolean is_z_x_y_dot_filetype) {
    return origin == null
        ? new FeatureListWithoutOffset(vggAnnotations, imageResolution, is_z_x_y_dot_filetype).get()
        : new FeatureListWithOffset(vggAnnotations, imageResolution, is_z_x_y_dot_filetype, origin)
            .get();
  }

  public Set<TiledPolygon> getTiledPolygons(boolean z_x_y_dot_filetype) {
    return toTiledPolygons(new TilingConf(20, 1024), vggAnnotations, z_x_y_dot_filetype);
  }

  public Set<Polygon> getPolygons() {
    return features.stream()
        .map(
            feature -> {
              var polygon = feature.geometry();
              var metadata = new HashMap<String, String>();
              metadata.put("filename", feature.filename());
              metadata.put("label", feature.label());
              metadata.put("confidence", Double.toString(feature.confidence()));
              polygon.setUserData(metadata);
              return polygon;
            })
        .collect(toSet());
  }
}
