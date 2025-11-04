package app.bpartners.geojobs.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class KeyPredicateFunction {

  @SafeVarargs
  public final <T> Predicate<T> apply(Function<? super T, ?>... keyExtractors) {
    final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();
    return elt -> {
      final List<?> keys =
          Arrays.stream(keyExtractors).map(key -> key.apply(elt)).collect(Collectors.toList());
      return seen.putIfAbsent(keys, Boolean.TRUE) == null;
    };
  }
}
