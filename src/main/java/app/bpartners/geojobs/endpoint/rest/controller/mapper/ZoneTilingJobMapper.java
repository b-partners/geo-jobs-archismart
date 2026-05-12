package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob.ZoomLevelEnum.fromValue;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ParcelService;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ZoneTilingJobMapper {
  private final ParcelService parcelService;
  private final StatusMapper<JobStatus> statusMapper;
  private final ZoomMapper zoomMapper;

  public ZoneTilingJob toDomain(CreateZoneTilingJob rest, Boolean isSynchronous) {
    var generatedId = randomUUID();
    var job =
        ZoneTilingJob.builder()
            .id(generatedId.toString())
            .zoneName(rest.getZoneName())
            .emailReceiver(rest.getEmailReceiver())
            .isRooferMade(isSynchronous != null && isSynchronous)
            .submissionInstant(now())
            .build();
    job.hasNewStatus(
        JobStatus.builder()
            .health(UNKNOWN)
            .progression(PENDING)
            .creationDatetime(now())
            .jobId(generatedId.toString())
            .build());
    return job;
  }

  public app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(
      ZoneTilingJob domain, List<ParcelTilingTask> parcelTilingTaskList) {
    var parcels =
        parcelService.getParcelsByJobId(domain.getId()).stream()
            .map(ParcelTask::getParcel)
            .toList();
    return toRest(domain, parcelTilingTaskList, parcels);
  }

  public app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(
      ZoneTilingJob domain, List<ParcelTilingTask> parcelTilingTaskList, boolean jobNotSaved) {
    List<Parcel> parcels =
        jobNotSaved
            ? List.of()
            : parcelService.getParcelsByJobId(domain.getId()).stream()
                .map(ParcelTask::getParcel)
                .toList();
    return toRest(domain, parcelTilingTaskList, parcels);
  }

  private app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob toRest(
      ZoneTilingJob domain, List<ParcelTilingTask> parcelTilingTaskList, List<Parcel> parcels) {
    var parcel0 = parcels.isEmpty() ? null : parcels.getFirst(); // only need one
    var parcelContent = parcel0 == null ? null : parcel0.getParcelContent();

    var zoom =
        parcelContent == null
            ? null
            : parcelContent.getFeature() == null ? null : parcelContent.getFeature().getZoom();
    return new app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob()
        .id(domain.getId())
        .zoneName(domain.getZoneName())
        .creationDatetime(domain.getSubmissionInstant())
        .zoomLevel(
            parcel0 == null
                ? null
                : (parcelContent.restFeatures() == null
                    ? null
                    : zoom == null
                        ? null
                        : zoomMapper.toRest(ArcgisImageZoom.fromZoomLevel((Integer) zoom))))

        // All parcels of the same job have same geoServer url and parameter
        .geoServerUrl(parcel0 == null ? null : parcelContent.getGeoServerUrl().toString())
        .geoServerParameter(parcel0 == null ? null : parcelContent.getGeoServerParameter())
        .emailReceiver(domain.getEmailReceiver())
        .features(parcelTilingTaskList.stream().map(FeatureMapper::from).toList())
        .status(statusMapper.toRest(domain.getStatus()));
  }

  public CreateZoneTilingJob from(Detection detection) {
    var featureList = detection.getTilingRestFeatures();
    var zoom =
        featureList.isEmpty() || featureList.getFirst().getProperties() == null
            ? HOUSES_0.getZoomLevel()
            : featureList.getFirst().getProperties().get("zoom") == null
                ? HOUSES_0.getZoomLevel()
                : (Integer) featureList.getFirst().getProperties().get("zoom");
    var overallConfiguration = detection.getGeoServerProperties();
    return new CreateZoneTilingJob()
        .emailReceiver(detection.getEmailReceiver())
        .zoneName(detection.getZoneName())
        .geoServerParameter(overallConfiguration.getGeoServerParameter())
        .geoServerUrl(overallConfiguration.getGeoServerUrl())
        .features(featureList)
        .zoomLevel(fromValue(ArcgisImageZoom.fromZoomLevel(zoom).name()));
  }
}
