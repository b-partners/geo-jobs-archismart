package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusRecomputingSubmitted;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionJobStatusRecomputingSubmittedService
    implements Consumer<GeoJsonConversionJobStatusRecomputingSubmitted> {
  private final GeoJsonConversionJobStatusRecomputingSubmittedBean service;

  @Override
  public void accept(GeoJsonConversionJobStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
