package app.bpartners.geojobs.service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.only;

import app.bpartners.geojobs.endpoint.event.model.DetectionCreated;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.detection.DetectionTilingCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionCreatedServiceTest {
  DetectionTilingCreation detectionTilingCreationMock = mock();
  DetectionCreatedService subject = new DetectionCreatedService(detectionTilingCreationMock);

  @BeforeEach
  void setUp() {
    when(detectionTilingCreationMock.apply(any())).thenReturn(mock());
  }

  @Test
  void detection_create_does_not_throw_exception() {
    Detection detection = mock();

    subject.accept(new DetectionCreated(detection));

    verify(detectionTilingCreationMock, only()).apply(detection);
  }
}
