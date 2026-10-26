package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.lidar.LidarProcessorType.DEFAULT;
import static app.bpartners.geojobs.model.lidar.LidarProcessorType.THREE_D_BAG_ROOFER;

import app.bpartners.geojobs.model.lidar.LidarProcessorType;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONSafeModeProcessor implements Function<CityJSONRequest, List<CityJSON>> {
  private final CityJSON3DBagRooferProcessor rooferProcessor;
  private final CityJSONThreedProcessor internalProcessor;
  private final CityJSONRequestRepository cityJSONRequestRepository;

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    List<CityJSON> cityJsons;
    try {
      cityJsons = rooferProcessor.apply(request);
      setCityJSONProcessor(request, THREE_D_BAG_ROOFER);
    } catch (Exception e) {
      log.error("Roofer CityJSON generation failed. Falling back to the internal processor.", e);
      cityJsons = internalProcessor.apply(request);
      setCityJSONProcessor(request, DEFAULT);
    }
    return cityJsons;
  }

  private void setCityJSONProcessor(CityJSONRequest request, LidarProcessorType processorType) {
    cityJSONRequestRepository.save(request.toBuilder().lidarProcessorType(processorType).build());
  }
}
