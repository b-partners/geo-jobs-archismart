package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobStatusRecomputingSubmitted;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAddressConversionJobStatusRecomputingSubmittedService
    implements Consumer<DetectionAddressConversionJobStatusRecomputingSubmitted> {
  private final DetectionAddressConversionJobStatusRecomputingSubmittedBean jobStatusService;

  @Override
  public void accept(DetectionAddressConversionJobStatusRecomputingSubmitted event) {
    jobStatusService.accept(event);
  }
}
