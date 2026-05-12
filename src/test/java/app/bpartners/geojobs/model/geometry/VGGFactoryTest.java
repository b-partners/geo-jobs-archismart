package app.bpartners.geojobs.model.geometry;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.service.GeometryPixelProjector;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.TileCoordinatesPolygonIntersection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class VGGFactoryTest {
  GeometryConverter geometryConverter = new GeometryConverter(null, null);
  TileCoordinatesPolygonIntersection tileCoordinatesPolygonIntersection =
      new TileCoordinatesPolygonIntersection(new GeometryPixelProjector(), geometryConverter);
  GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();
  ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  DetectionBoundaryMerger merger = new DetectionBoundaryMerger();

  private final VGGFactory subject =
      new VGGFactory(tileCoordinatesPolygonIntersection, geometryConverter, merger);

  @SneakyThrows
  @Test
  void unify_vgg() {
    var expectedFile =
        new ClassPathResource("geometry/vgg/bati_4_polygons_merged-vgg.json").getFile();
    var expectedFileContent = Files.readString(expectedFile.toPath());
    var expected = objectMapper.readValue(expectedFileContent, new TypeReference<VGG>() {});
    var vggFile = new ClassPathResource("/geometry/vgg/bati_4_polygons-vgg.json").getFile();
    var vggFileContent = Files.readString(vggFile.toPath());
    var vgg = objectMapper.readValue(vggFileContent, new TypeReference<Set<VGG>>() {});

    var actual = subject.unifyVggSet(vgg);

    var expectedAnnotation = expected.get("c2f4b6f0-5d5e-4b7e-9c1a-1f3e8c7d9a21");
    var actualAnnotation = actual.iterator().next().get("c2f4b6f0-5d5e-4b7e-9c1a-1f3e8c7d9a21");
    assertEquals(
        expectedAnnotation.getRegions().values().toString(),
        actualAnnotation.getRegions().values().toString());
  }
}
