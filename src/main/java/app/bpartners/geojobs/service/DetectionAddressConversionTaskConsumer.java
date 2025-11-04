package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.mapper.AreaPictureDetailsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionAddressConversionTaskConsumer
    implements TaskConsumer<DetectionAddressConversionTask> {
  private final AreaPictureApi areaPictureApi;
  private final AreaPictureDetailsMapper areaPictureDetailsMapper;

  @Override
  public void accept(DetectionAddressConversionTask task) {
    var address = task.getAddress();
    var e2ApiKey = task.getE2ApiKey();
    var crupdateAreaPictureDetails = areaPictureDetailsMapper.toCrupdateAreaPictureDetails(address);

    var areaPictureId = randomUUID().toString();
    var areaPictureDetails =
        areaPictureApi.crupdateAreaPictureDetails(
            areaPictureId, crupdateAreaPictureDetails, e2ApiKey);

    var feature = areaPictureDetailsMapper.toFeature(areaPictureDetails, address);
    task.setFeature(feature);
    task.setLayer(areaPictureDetails.actualLayer().name());
  }
}
