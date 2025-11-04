package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.service.geojson.GeoJson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeoFeatureConverter implements Function<List<File>, List<GeoJson.GeoFeature>> {
  private final ObjectMapper objectMapper;

  @Override
  public List<GeoJson.GeoFeature> apply(List<File> partialConvertedGeoJsonFiles) {
    return partialConvertedGeoJsonFiles.stream().map(this::apply).flatMap(List::stream).toList();
  }

  public List<GeoJson.GeoFeature> apply(File file) {
    try {
      return objectMapper.readValue(file, new TypeReference<>() {});
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
