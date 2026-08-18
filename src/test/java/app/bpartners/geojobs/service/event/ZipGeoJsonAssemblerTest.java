package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.DetectionBackgroundRetriever;
import app.bpartners.geojobs.service.DetectionService;
import app.bpartners.geojobs.service.DetectionZoneToProcessProvider;
import app.bpartners.geojobs.service.GeoFeatureConverter;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;

class ZipGeoJsonAssemblerTest {
  GeoJsonConversionTaskRepository geoJsonConversionTaskRepositoryMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectionService detectionServiceMock = mock();
  EventProducer eventProducerMock = mock();
  GeoFeatureConverter geoFeatureConverter = new GeoFeatureConverter(new ObjectMapper());
  DetectionZoneToProcessProvider detectionProvidedZoneUnifierMock = mock();
  GeometryConverter geometryConverter = new GeometryConverter();
  DetectionBackgroundRetriever detectionBackgroundRetrieverMock = mock();
  GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();

  ZipGeoJsonAssembler subject =
      new ZipGeoJsonAssembler(
          geoJsonConversionTaskRepositoryMock,
          geoJsonConversionJobRepositoryMock,
          bucketComponentMock,
          zoneDetectionJobServiceMock,
          detectionRepositoryMock,
          detectionServiceMock,
          eventProducerMock,
          geoFeatureConverter,
          detectionProvidedZoneUnifierMock,
          geometryConverter,
          detectionBackgroundRetrieverMock,
          geometrySquareMeterArea);

  @Test
  void toiture_revetement_geojson_carries_roof_result_properties_without_vgg() throws Exception {
    var conversionJobId = randomUUID().toString();
    var zdjId = randomUUID().toString();
    var delimitationProperties =
        new HashMap<String, Object>(
            Map.of(
                "covering",
                "{\"primary\":\"ROOF_TUILES\",\"secondary\":\"ROOF_ARDOISE\"}",
                "usure_rate",
                12.5,
                "humidite_rate",
                3.0,
                "moisissure_rate",
                1.0,
                "global_rate_value",
                4.2,
                "global_rate_type",
                "MOYEN",
                "revetement_1",
                "ROOF_TUILES",
                "revetement_2",
                "ROOF_ARDOISE"));
    var delimitationFeature =
        Feature.builder()
            .properties(delimitationProperties)
            .geometry(
                new Feature.FeatureGeometry(
                    POLYGON,
                    "{\"coordinates\":[[[2.35,48.85],[2.36,48.85],[2.36,48.84],[2.35,48.84],"
                        + "[2.35,48.85]]],\"type\":\"Polygon\"}"))
            .build();
    var detection =
        Detection.builder()
            .id(randomUUID().toString())
            .zdjId(zdjId)
            .needsImageOutput(false)
            .detectableObjectModelList(List.of(new DetectableObjectModel().modelName(TOITURE)))
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        Feature.builder().id("F1").build(), List.of(delimitationFeature))))
            .build();

    var conversionJob =
        GeoJsonConversionJob.builder().id(conversionJobId).zoneDetectionJobId(zdjId).build();
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    when(zoneDetectionJobMock.getId()).thenReturn(zdjId);
    when(zoneDetectionJobMock.getZoneName()).thenReturn("zone-test");
    when(zoneDetectionJobMock.isFinished()).thenReturn(false);
    when(zoneDetectionJobServiceMock.findById(zdjId)).thenReturn(zoneDetectionJobMock);
    when(geoJsonConversionTaskRepositoryMock.findAllByJobId(conversionJobId)).thenReturn(List.of());
    when(detectionServiceMock.getByZoneDetectionJob(zoneDetectionJobMock)).thenReturn(detection);
    when(detectionProvidedZoneUnifierMock.apply(detection))
        .thenReturn(new GeometryFactory().createMultiPolygon());
    when(detectionBackgroundRetrieverMock.apply(detection)).thenReturn(null);
    when(geoJsonConversionJobRepositoryMock.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(() -> subject.accept(conversionJob));

    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponentMock).upload(fileCaptor.capture(), any());
    var toitureRevetementGeoJson =
        readZipEntry(fileCaptor.getValue(), "TOITURE_REVETEMENT.geojson");

    assertNotNull(
        toitureRevetementGeoJson, "The ZIP must contain a *_TOITURE_REVETEMENT.geojson entry");
    assertTrue(toitureRevetementGeoJson.contains("\"covering\""));
    assertTrue(toitureRevetementGeoJson.contains("\"global_rate_value\""));
    assertTrue(toitureRevetementGeoJson.contains("\"global_rate_type\""));
    assertTrue(toitureRevetementGeoJson.contains("\"usure_rate\""));
    assertTrue(toitureRevetementGeoJson.contains("\"humidite_rate\""));
    assertTrue(toitureRevetementGeoJson.contains("\"moisissure_rate\""));
    assertTrue(toitureRevetementGeoJson.contains("\"revetement_1\""));
    assertTrue(toitureRevetementGeoJson.contains("\"revetement_2\""));
  }

  private static String readZipEntry(File zipFile, String entryNameSuffix) throws Exception {
    try (var zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().endsWith(entryNameSuffix)) {
          return new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
    }
    return null;
  }
}
