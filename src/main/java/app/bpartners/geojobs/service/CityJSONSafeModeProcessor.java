package app.bpartners.geojobs.service;

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
  private final CityJSONInternalProcessor internalProcessor;

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    try {
      return rooferProcessor.apply(request);
    } catch (Exception e) {
      log.error("Roofer CityJSON generation failed. Falling back to the internal processor.", e);

      return internalProcessor.apply(request);
    }
  }
}
