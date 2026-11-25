package app.bpartners.geojobs.service.detection.inference;

import java.util.HashMap;
import java.util.List;

public final class ModelCardMappingValidator {
  private ModelCardMappingValidator() {}

  public static void validate(List<ModelCardMapping> mappings) {
    var firstMappingByModelCardAndResolution = new HashMap<List<Object>, ModelCardMapping>();
    var firstMappingByTypeAndResolution = new HashMap<List<Object>, ModelCardMapping>();
    for (var mapping : mappings) {
      if (mapping.zoomLevel() == null || mapping.zoomLevel() <= 0) {
        throw new IllegalStateException("Invalid zoomLevel in inference conf: " + mapping);
      }
      if (mapping.spatialResolution() == null || mapping.spatialResolution() <= 0) {
        throw new IllegalStateException("Invalid spatialResolution in inference conf: " + mapping);
      }
      if (mapping.inferenceSpatialResolution() != null
          && mapping.inferenceSpatialResolution() <= 0) {
        throw new IllegalStateException(
            "Invalid inferenceSpatialResolution in inference conf: " + mapping);
      }
      var first =
          firstMappingByModelCardAndResolution.putIfAbsent(
              List.of(mapping.modelCardId(), mapping.spatialResolution()), mapping);
      if (first != null && !first.zoomLevel().equals(mapping.zoomLevel())) {
        throw new IllegalStateException(
            "Model card "
                + mapping.modelCardId()
                + " is mapped with different zoomLevel for the same spatialResolution: "
                + first
                + " and "
                + mapping);
      }
      var duplicate =
          firstMappingByTypeAndResolution.putIfAbsent(
              List.of(mapping.objectType(), mapping.spatialResolution()), mapping);
      if (duplicate != null) {
        throw new IllegalStateException(
            "Object type "
                + mapping.objectType()
                + " is mapped several times for spatialResolution "
                + mapping.spatialResolution()
                + ": "
                + duplicate
                + " and "
                + mapping);
      }
    }
  }
}
