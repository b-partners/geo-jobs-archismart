package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileAddressConverted;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileSaved;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.ExcelAddressConverter;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionExcelFileSavedService implements Consumer<DetectionExcelFileSaved> {
  private final BucketComponent bucketComponent;
  private final ExcelAddressConverter excelAddressConverter;
  private final DetectionRepository detectionRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(DetectionExcelFileSaved event) {
    var detection = event.getDetection();
    var excelFile = bucketComponent.download(detection.getExcelFileKey());
    var retrievedAddresses = excelAddressConverter.apply(excelFile);
    var savedDetection =
        detectionRepository.save(
            detection.toBuilder().convertedAddresses(retrievedAddresses).build());

    eventProducer.accept(
        List.of(DetectionExcelFileAddressConverted.builder().detection(savedDetection).build()));
  }
}
