package app.bpartners.geojobs.service.dashboard.component;

import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob;

public record CrupdateAreaPictureDetails(
    String address,
    Integer shiftNb,
    Boolean isExtended,
    String fileId,
    String filename,
    String prospectId,
    ZoneTilingJob.ZoomLevelEnum zoomLevel) {}
