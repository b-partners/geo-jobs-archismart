package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.model.ZoneTilingJob.ZoomLevelEnum;
import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import org.springframework.stereotype.Component;

@Component
public class ZoomMapper {
  public ZoomLevelEnum toRest(ArcgisImageZoom zoom) {
    return ZoomLevelEnum.valueOf(zoom.toString());
  }

  public ArcgisImageZoom toDomain(CreateZoneTilingJob.ZoomLevelEnum zoomLevel) {
    return ArcgisImageZoom.valueOf(zoomLevel.name());
  }
}
