package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ImportedZoneTilingJobSaved;
import app.bpartners.geojobs.endpoint.rest.model.BucketSeparatorType;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.ImportedZoneTilingJobSavedService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.S3Object;

//  TODO: enable accept_truncated_ok
@Disabled
public class ImportedZoneTilingJobSavedServiceTest {
  CustomBucketComponent customBucketComponentMock = mock();
  ZoneTilingJobService tilingJobServiceMock = mock();
  TilingTaskRepository tilingTaskRepositoryMock = mock();
  ImportedZoneTilingJobSavedService subject =
      new ImportedZoneTilingJobSavedService(
          customBucketComponentMock, tilingJobServiceMock, tilingTaskRepositoryMock);

  @Test
  void accept_object_name_ok() {
    Long startFrom = 0L;
    Long endAt = null;
    String jobId = "jobId";
    String dummyBucketName = "dummyBucketName";
    String dummyBucketPrefix = "dummyBucketPrefix";
    GeoServerParameter geoServerParameter = new GeoServerParameter();
    String dummyGeoServerUrl = "https://dummyGeoServerUrl.com";
    List<S3Object> s3Objects =
        List.of(
            S3Object.builder().key("defaultPath/20/fusionAll/100_200").build(),
            S3Object.builder().key("defaultPath/20/fusionAll/200_200").build());
    when(tilingJobServiceMock.findById(jobId))
        .thenReturn(
            ZoneTilingJob.builder()
                .id(jobId)
                .zoneName("dummyZoneName")
                .emailReceiver("dummyEmailReceiver")
                .statusHistory(
                    List.of(
                        JobStatus.builder()
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(now())
                            .build()))
                .build());

    when(customBucketComponentMock.listObjects(dummyBucketName, dummyBucketPrefix))
        .thenReturn(s3Objects);

    subject.accept(
        new ImportedZoneTilingJobSaved(
            startFrom,
            endAt,
            jobId,
            dummyBucketName,
            dummyBucketPrefix,
            geoServerParameter,
            dummyGeoServerUrl,
            BucketSeparatorType.UNDERSCORE));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(tilingTaskRepositoryMock, times(1)).saveAll(listCaptor.capture());
    List<ParcelTilingTask> savedParcelTilingTasks = (List<ParcelTilingTask>) listCaptor.getValue();
    var firstTask = savedParcelTilingTasks.getFirst();
    var lastTask = savedParcelTilingTasks.getLast();
    assertEquals(1, savedParcelTilingTasks.size());
    Tile firstTile = firstTask.getParcelContent().getFirstTile();
    Tile lastTile = lastTask.getParcelContent().getFirstTile();
    assertEquals(s3Objects.size(), savedParcelTilingTasks.size());
    assertTrue(savedParcelTilingTasks.stream().allMatch(ParcelTilingTask::isSucceeded));
    assertEquals(
        Tile.builder()
            .id(firstTile.getId())
            .creationDatetime(firstTile.getCreationDatetime())
            .bucketPath("defaultPath/20/fusionAll/100_200")
            .coordinates(new TileCoordinates().x(100).y(200).z(20))
            .build(),
        firstTile);
    assertEquals(
        Tile.builder()
            .id(lastTile.getId())
            .creationDatetime(lastTile.getCreationDatetime())
            .bucketPath("defaultPath/20/fusionAll/200_200")
            .coordinates(new TileCoordinates().x(200).y(200).z(20))
            .build(),
        lastTile);
  }

  @Test
  void accept_object_slash_splitter_ok() {
    Long startFrom = 0L;
    Long endAt = null;
    String jobId = "jobId";
    String dummyBucketName = "dummyBucketName";
    String dummyBucketPrefix = "dummyBucketPrefix";
    GeoServerParameter geoServerParameter = new GeoServerParameter();
    String dummyGeoServerUrl = "https://dummyGeoServerUrl.com";
    List<S3Object> s3Objects =
        List.of(
            S3Object.builder().key("s3ObjectKeyLayer1/20/100/200").build(),
            S3Object.builder().key("s3ObjectKeyLayer2/20/200/200").build());
    when(tilingJobServiceMock.findById(jobId))
        .thenReturn(
            ZoneTilingJob.builder()
                .id(jobId)
                .zoneName("dummyZoneName")
                .emailReceiver("dummyEmailReceiver")
                .statusHistory(
                    List.of(
                        JobStatus.builder()
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(now())
                            .build()))
                .build());

    when(customBucketComponentMock.listObjects(dummyBucketName, dummyBucketPrefix))
        .thenReturn(s3Objects);

    subject.accept(
        new ImportedZoneTilingJobSaved(
            startFrom,
            endAt,
            jobId,
            dummyBucketName,
            dummyBucketPrefix,
            geoServerParameter,
            dummyGeoServerUrl,
            BucketSeparatorType.SLASH));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(tilingTaskRepositoryMock, times(1)).saveAll(listCaptor.capture());
    List<ParcelTilingTask> savedParcelTilingTasks = (List<ParcelTilingTask>) listCaptor.getValue();
    var firstTask = savedParcelTilingTasks.getFirst();
    var lastTask = savedParcelTilingTasks.getLast();
    assertEquals(1, savedParcelTilingTasks.size());
    Tile firstTile = firstTask.getParcelContent().getFirstTile();
    Tile lastTile = lastTask.getParcelContent().getFirstTile();
    assertEquals(s3Objects.size(), savedParcelTilingTasks.size());
    assertTrue(savedParcelTilingTasks.stream().allMatch(ParcelTilingTask::isSucceeded));
    assertEquals(
        Tile.builder()
            .id(firstTile.getId())
            .creationDatetime(firstTile.getCreationDatetime())
            .bucketPath("s3ObjectKeyLayer1/20/100/200")
            .coordinates(new TileCoordinates().x(100).y(200).z(20))
            .build(),
        firstTile);
    assertEquals(
        Tile.builder()
            .id(lastTile.getId())
            .creationDatetime(lastTile.getCreationDatetime())
            .bucketPath("s3ObjectKeyLayer2/20/200/200")
            .coordinates(new TileCoordinates().x(200).y(200).z(20))
            .build(),
        lastTile);
  }

  @Test
  void accept_truncated_ok() {
    Long startFrom = 0L;
    Long endAt = 1L;
    String jobId = "jobId";
    String dummyBucketName = "dummyBucketName";
    String dummyBucketPrefix = "dummyBucketPrefix";
    GeoServerParameter geoServerParameter = new GeoServerParameter();
    String dummyGeoServerUrl = "https://dummyGeoServerUrl.com";
    List<S3Object> s3Objects =
        List.of(
            S3Object.builder().key("s3ObjectKeyLayer1/20/100/200").build(),
            S3Object.builder().key("s3ObjectKeyLayer2/20/100/200").build());
    when(tilingJobServiceMock.findById(jobId))
        .thenReturn(
            ZoneTilingJob.builder()
                .id(jobId)
                .zoneName("dummyZoneName")
                .emailReceiver("dummyEmailReceiver")
                .statusHistory(
                    List.of(
                        JobStatus.builder()
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(now())
                            .build()))
                .build());

    when(customBucketComponentMock.listObjects(dummyBucketName, dummyBucketPrefix))
        .thenReturn(s3Objects);

    subject.accept(
        new ImportedZoneTilingJobSaved(
            startFrom,
            endAt,
            jobId,
            dummyBucketName,
            dummyBucketPrefix,
            geoServerParameter,
            dummyGeoServerUrl,
            BucketSeparatorType.SLASH));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(tilingTaskRepositoryMock, times(1)).saveAll(listCaptor.capture());
    List<ParcelTilingTask> savedParcelTilingTasks = (List<ParcelTilingTask>) listCaptor.getValue();
    var firstTask = savedParcelTilingTasks.getFirst();
    assertEquals(1, savedParcelTilingTasks.size());
    Tile firstTile = firstTask.getParcelContent().getFirstTile();
    assertEquals(1, savedParcelTilingTasks.size());
    assertTrue(savedParcelTilingTasks.stream().allMatch(ParcelTilingTask::isSucceeded));
    assertEquals(
        Tile.builder()
            .id(firstTile.getId())
            .creationDatetime(firstTile.getCreationDatetime())
            .bucketPath("s3ObjectKeyLayer1/20/100/200")
            .coordinates(new TileCoordinates().x(100).y(200).z(20))
            .build(),
        firstTile);
  }

  @Test
  void accept_truncated_all_ok() {
    Long startFrom = 0L;
    Long endAt = null;
    String jobId = "jobId";
    String dummyBucketName = "dummyBucketName";
    String dummyBucketPrefix = "dummyBucketPrefix";
    GeoServerParameter geoServerParameter = new GeoServerParameter();
    String dummyGeoServerUrl = "https://dummyGeoServerUrl.com";
    List<S3Object> s3Objects =
        List.of(
            S3Object.builder().key("s3ObjectKeyLayer1/20/100/200").build(),
            S3Object.builder().key("s3ObjectKeyLayer2/20/200/200").build(),
            S3Object.builder().key("s3ObjectKeyLayer1/20/100/900").build());
    when(tilingJobServiceMock.findById(jobId))
        .thenReturn(
            ZoneTilingJob.builder()
                .id(jobId)
                .zoneName("dummyZoneName")
                .emailReceiver("dummyEmailReceiver")
                .statusHistory(
                    List.of(
                        JobStatus.builder()
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .creationDatetime(now())
                            .build()))
                .build());

    when(customBucketComponentMock.listObjects(dummyBucketName, dummyBucketPrefix))
        .thenReturn(s3Objects);

    subject.accept(
        new ImportedZoneTilingJobSaved(
            startFrom,
            endAt,
            jobId,
            dummyBucketName,
            dummyBucketPrefix,
            geoServerParameter,
            dummyGeoServerUrl,
            BucketSeparatorType.SLASH));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(tilingTaskRepositoryMock, times(1)).saveAll(listCaptor.capture());
    List<ParcelTilingTask> savedParcelTilingTasks = (List<ParcelTilingTask>) listCaptor.getValue();
    assertEquals(2, savedParcelTilingTasks.size());
    assertEquals(2, savedParcelTilingTasks.size());
    assertTrue(savedParcelTilingTasks.stream().allMatch(ParcelTilingTask::isSucceeded));
    assertEquals(1, savedParcelTilingTasks.getFirst().getParcelContent().getTiles().size());
    assertEquals(1, savedParcelTilingTasks.getLast().getParcelContent().getTiles().size());
  }

  @Test
  void convert_tiles_from_bucket_path_ko() {
    assertThrows(ApiException.class, () -> subject.fromBucketPathKey("dummyBucket"));
  }

  @Test
  void convert_tiles_from_object_name_ko() {
    assertThrows(ApiException.class, () -> subject.fromObjectName("dummyBucket"));
  }
}
