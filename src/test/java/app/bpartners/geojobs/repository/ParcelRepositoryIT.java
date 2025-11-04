package app.bpartners.geojobs.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ParcelRepositoryIT extends FacadeIT {
  @Autowired ParcelRepository subject;

  Parcel toSave() {
    return Parcel.builder()
        .id("parcel_id")
        .parcelContent(
            ParcelContent.builder()
                .id("parcel_content_id")
                .feature(
                    Feature.builder()
                        .id("feature_id")
                        .zoom(20)
                        .geometry(
                            Feature.FeatureGeometry.builder()
                                .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                                .actualInstanceStringValue(
                                    "{\"type\": \"MultiPolygon\",\"coordinates\": [ [ [ ["
                                        + " 7.013274594521259, 43.550967070215918 ], ["
                                        + " 7.014296384502766, 43.551202851619735 ], ["
                                        + " 7.014722512163215, 43.551199530761302 ], ["
                                        + " 7.014878300770262, 43.550913936251106 ], ["
                                        + " 7.013448711199724, 43.550661549278466 ], ["
                                        + " 7.013274594521259, 43.550967070215918 ] ] ] ]}")
                                .build())
                        .build())
                .build())
        .build();
  }

  @Test
  void read_parcel_ok() {
    var parcel = subject.save(toSave());

    var actual = getById(toSave().getId());

    assertEquals(parcel, actual);
  }

  private Parcel getById(String id) {
    return subject.findById(id).orElseThrow(() -> new NotFoundException("Parcel not found"));
  }
}
