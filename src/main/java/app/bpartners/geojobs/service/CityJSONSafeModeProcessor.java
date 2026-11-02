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
  private final CityJSONThreedProcessor threedProcessor;
  private final CityJSONRequestRepository cityJSONRequestRepository;

  public record SafeModeResult(List<CityJSON> cityJsons, LidarProcessorType processorType) {}

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    var result = generate(request);
    setCityJSONProcessor(request, result.processorType());
    return result.cityJsons();
  }

  /**
   * Same generation as {@link #apply}, without recording the processor used on the request: the
   * request does not need to be persisted, e.g. when the CityJSON is only generated to read roof
   * metrics from it.
   */
  public SafeModeResult generate(CityJSONRequest request) {
    try {
      return new SafeModeResult(rooferProcessor.apply(request), THREE_D_BAG_ROOFER);
    } catch (Exception e) {
      log.error("Roofer CityJSON generation failed. Falling back to threed.", e);
      return new SafeModeResult(threedProcessor.apply(request), DEFAULT);
    }
  }

  private void setCityJSONProcessor(CityJSONRequest request, LidarProcessorType processorType) {
    cityJSONRequestRepository.save(request.toBuilder().lidarProcessorType(processorType).build());
  }
}
