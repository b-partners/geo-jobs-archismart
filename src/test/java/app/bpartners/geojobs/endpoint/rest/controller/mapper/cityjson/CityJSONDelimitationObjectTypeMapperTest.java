package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.model.DelimitationObjectType;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType;
import org.junit.jupiter.api.Test;

class CityJSONDelimitationObjectTypeMapperTest {

  @Test
  void from_rest_null() {
    assertEquals(
        CityJSONDelimitationObjectType.BUILDING_ROOF,
        CityJSONDelimitationObjectTypeMapper.fromRestDelimitationObjectType(null));
  }

  @Test
  void from_rest_building_roof() {
    assertEquals(
        CityJSONDelimitationObjectType.BUILDING_ROOF,
        CityJSONDelimitationObjectTypeMapper.fromRestDelimitationObjectType(
            DelimitationObjectType.BUILDING_ROOF));
  }

  @Test
  void from_rest_building_roof_segment_face() {
    assertEquals(
        CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE,
        CityJSONDelimitationObjectTypeMapper.fromRestDelimitationObjectType(
            DelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE));
  }

  @Test
  void to_rest_null() {
    assertEquals(
        DelimitationObjectType.BUILDING_ROOF,
        CityJSONDelimitationObjectTypeMapper.toRestDelimitationObjectType(null));
  }

  @Test
  void to_rest_building_roof() {
    assertEquals(
        DelimitationObjectType.BUILDING_ROOF,
        CityJSONDelimitationObjectTypeMapper.toRestDelimitationObjectType(
            CityJSONDelimitationObjectType.BUILDING_ROOF));
  }

  @Test
  void to_rest_building_roof_segment_face() {
    assertEquals(
        DelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE,
        CityJSONDelimitationObjectTypeMapper.toRestDelimitationObjectType(
            CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE));
  }
}
