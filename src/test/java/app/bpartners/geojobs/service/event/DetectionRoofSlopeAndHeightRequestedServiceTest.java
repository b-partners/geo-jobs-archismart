package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_4;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionRoofSlopeAndHeightRequestedServiceTest {
  DetectionRepository detectionRepositoryMock = mock();
  EventProducer eventProducerMock = mock();

  DetectionRoofSlopeAndHeightRequestedService subject =
      new DetectionRoofSlopeAndHeightRequestedService(detectionRepositoryMock, eventProducerMock);

  @Test
  void produces_feature_roof_slope_and_height_requested_event() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    var providedFeatureOneMock = mock(Feature.class);
    var providedFeatureTwoMock = mock(Feature.class);

    when(detectionMock.getFeatureWithDelimitations()).thenReturn(mock());
    when(detectionMock.getProvidedGeoJsonZone())
        .thenReturn(List.of(providedFeatureOneMock, providedFeatureTwoMock));
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(
        () ->
            subject.accept(
                DetectionRoofSlopeAndHeightRequested.builder()
                    .detectionId(detectionIdentifier)
                    .build()));

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(eventListCaptor.capture());
    var featureOneRoofPropertiesEvent =
        (FeatureRoofSlopeAndHeightRequested) eventListCaptor.getAllValues().getFirst().getFirst();
    var featureTwoRoofPropertiesEvent =
        (FeatureRoofSlopeAndHeightRequested) eventListCaptor.getAllValues().getLast().getFirst();
    assertEquals(
        new FeatureRoofSlopeAndHeightRequested(detectionIdentifier, providedFeatureOneMock, 0),
        featureOneRoofPropertiesEvent);
    assertEquals(
        new FeatureRoofSlopeAndHeightRequested(detectionIdentifier, providedFeatureTwoMock, 1),
        featureTwoRoofPropertiesEvent);
    assertEquals(EVENT_STACK_4, featureOneRoofPropertiesEvent.getEventStack());
    assertEquals(EVENT_STACK_4, featureTwoRoofPropertiesEvent.getEventStack());
  }

  @Test
  void throw_when_detection_not_found() {
    var detectionId = "missing-id";
    var requested = DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionId).build();

    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.empty());

    var error = assertThrows(RuntimeException.class, () -> subject.accept(requested));
    assertTrue(error.getMessage().contains("Detection={" + detectionId + "} not found"));
  }

  @Test
  void throw_when_polygon_delimitation_null() {
    var detectionId = "detection-id";
    var requested = DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionId).build();
    var detectionMock = mock(Detection.class);

    when(detectionMock.getFeatureWithDelimitations()).thenReturn(null);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    var error = assertThrows(RuntimeException.class, () -> subject.accept(requested));
    assertTrue(
        error
            .getMessage()
            .contains("FeatureWithDelimitation is null for detection={" + detectionId + "}"));
  }
}
