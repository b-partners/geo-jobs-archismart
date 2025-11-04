package app.bpartners.geojobs.service.cityjson.factory;

import java.util.List;
import java.util.Map;
import org.citygml4j.core.model.core.AbstractGenericAttribute;
import org.citygml4j.core.model.core.AbstractGenericAttributeProperty;
import org.citygml4j.core.model.generics.DoubleAttribute;
import org.citygml4j.core.model.generics.IntAttribute;
import org.citygml4j.core.model.generics.StringAttribute;

public class GenericAttributeFactory {
  private GenericAttributeFactory() {}

  public static List<AbstractGenericAttributeProperty> make(Map<String, Object> properties) {
    return properties.entrySet().stream()
        .map(entry -> makeGenericAttribute(entry.getKey(), entry.getValue()))
        .map(AbstractGenericAttributeProperty::new)
        .toList();
  }

  private static AbstractGenericAttribute<?> makeGenericAttribute(String key, Object value) {
    return switch (value) {
      case Double doubleValue -> new DoubleAttribute(key, doubleValue);
      case Integer intValue -> new IntAttribute(key, intValue);
      case String stringValue -> new StringAttribute(key, stringValue);
      default ->
          throw new IllegalArgumentException("Unsupported attribute type: " + value.getClass());
    };
  }
}
