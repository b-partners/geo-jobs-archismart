package app.bpartners.geojobs.service.cityjson.factory;

import app.bpartners.geojobs.service.cityjson.birdia.BirdiaPointOfContact;
import org.citygml4j.cityjson.model.metadata.Metadata;
import org.citygml4j.cityjson.model.metadata.ReferenceSystem;

public class MetadataFactory {
  private MetadataFactory() {}

  public static Metadata make(String id, String title, String crs) {
    var metadata = new Metadata();

    metadata.setIdentifier(id);
    metadata.setTitle(title);
    metadata.setPointOfContact(new BirdiaPointOfContact());
    metadata.setReferenceSystem(ReferenceSystem.parse(crs));

    return metadata;
  }
}
