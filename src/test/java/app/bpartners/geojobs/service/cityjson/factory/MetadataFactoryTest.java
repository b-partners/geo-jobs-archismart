package app.bpartners.geojobs.service.cityjson.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.citygml4j.cityjson.model.metadata.Metadata;
import org.junit.jupiter.api.Test;

class MetadataFactoryTest {

  @Test
  void make_with_epsg_2154_ok() {
    String id = "test-id";
    String title = "test-title";
    String crs = "EPSG:2154";

    Metadata metadata = MetadataFactory.make(id, title, crs);

    assertEquals(id, metadata.getIdentifier());
    assertEquals(title, metadata.getTitle());
    assertNotNull(metadata.getReferenceSystem());
  }

  @Test
  void make_with_epsg_2056_ok() {
    String id = "test-id-swiss";
    String title = "test-title-swiss";
    String crs = "EPSG:2056";

    Metadata metadata = MetadataFactory.make(id, title, crs);

    assertEquals(id, metadata.getIdentifier());
    assertEquals(title, metadata.getTitle());
    assertNotNull(metadata.getReferenceSystem());
  }
}
