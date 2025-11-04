package app.bpartners.geojobs.model;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomObjectMapper {
  private CustomObjectMapper() {}

  public static ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
    return objectMapper;
  }
}
