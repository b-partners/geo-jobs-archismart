package app.bpartners.geojobs.endpoint.rest.controller.v1.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.Status;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJob;
import app.bpartners.geojobs.repository.model.geocoding.GeoCodingJobStatus;
import org.junit.jupiter.api.Test;

class GeoCodingJobRestMapperTest {
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  GeoCodingJobRestMapper subject = new GeoCodingJobRestMapper(bucketComponentMock);

  @Test
  void to_rest_null_status_and_null_geojson_key() {
    var domain =
        GeoCodingJob.builder().endToEndId("endToEndId").geoJsonKey(null).status(null).build();

    var actual = subject.toRest(domain);

    assertEquals("endToEndId", actual.getId());
    assertNull(actual.getGeoJsonUrl());
    assertNull(actual.getStatus());
  }

  @Test
  void to_rest_null_status_ignores_message() {
    var domain =
        GeoCodingJob.builder()
            .endToEndId("endToEndId")
            .message("some message")
            .status(null)
            .build();

    var actual = subject.toRest(domain);

    assertNull(actual.getStatus());
  }

  @Test
  void to_rest_pending() {
    when(bucketComponentMock.presign("geoJsonKey")).thenReturn("presignedUrl");
    var domain =
        GeoCodingJob.builder()
            .endToEndId("endToEndId")
            .geoJsonKey("geoJsonKey")
            .status(GeoCodingJobStatus.PENDING)
            .build();

    var actual = subject.toRest(domain);

    assertEquals("presignedUrl", actual.getGeoJsonUrl());
    assertEquals(Status.ProgressionEnum.PENDING, actual.getStatus().getProgression());
    assertEquals(Status.HealthEnum.UNKNOWN, actual.getStatus().getHealth());
  }

  @Test
  void to_rest_processing() {
    var domain =
        GeoCodingJob.builder()
            .endToEndId("endToEndId")
            .status(GeoCodingJobStatus.PROCESSING)
            .build();

    var actual = subject.toRest(domain);

    assertEquals(Status.ProgressionEnum.PROCESSING, actual.getStatus().getProgression());
    assertEquals(Status.HealthEnum.UNKNOWN, actual.getStatus().getHealth());
  }

  @Test
  void to_rest_succeeded() {
    var domain =
        GeoCodingJob.builder()
            .endToEndId("endToEndId")
            .status(GeoCodingJobStatus.SUCCEEDED)
            .build();

    var actual = subject.toRest(domain);

    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStatus().getProgression());
    assertEquals(Status.HealthEnum.SUCCEEDED, actual.getStatus().getHealth());
  }

  @Test
  void to_rest_failed() {
    var domain =
        GeoCodingJob.builder().endToEndId("endToEndId").status(GeoCodingJobStatus.FAILED).build();

    var actual = subject.toRest(domain);

    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStatus().getProgression());
    assertEquals(Status.HealthEnum.FAILED, actual.getStatus().getHealth());
    assertNull(actual.getStatus().getMessage());
  }

  @Test
  void to_rest_failed_with_message() {
    var message = "Sheet index (3) is out of range (0..2)";
    var domain =
        GeoCodingJob.builder()
            .endToEndId("endToEndId")
            .message(message)
            .status(GeoCodingJobStatus.FAILED)
            .build();

    var actual = subject.toRest(domain);

    assertEquals(Status.ProgressionEnum.FINISHED, actual.getStatus().getProgression());
    assertEquals(Status.HealthEnum.FAILED, actual.getStatus().getHealth());
    assertEquals(message, actual.getStatus().getMessage());
  }

  @Test
  void to_rest_succeeded_with_message() {
    var message = "some message";
    var domain =
        GeoCodingJob.builder()
            .endToEndId("endToEndId")
            .message(message)
            .status(GeoCodingJobStatus.SUCCEEDED)
            .build();

    var actual = subject.toRest(domain);

    assertEquals(Status.HealthEnum.SUCCEEDED, actual.getStatus().getHealth());
    assertEquals(message, actual.getStatus().getMessage());
  }
}
