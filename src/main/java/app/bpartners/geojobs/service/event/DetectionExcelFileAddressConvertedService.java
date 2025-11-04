package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileAddressConverted;
import app.bpartners.geojobs.service.DetectionAddressConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionExcelFileAddressConvertedService
    implements Consumer<DetectionExcelFileAddressConverted> {
  private final DetectionAddressConsumer consumer;

  @Override
  public void accept(DetectionExcelFileAddressConverted event) {
    var detection = event.getDetection();
    consumer.accept(detection);
  }
}
