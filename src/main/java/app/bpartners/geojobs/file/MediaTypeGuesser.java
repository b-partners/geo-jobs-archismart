package app.bpartners.geojobs.file;

import java.util.function.Function;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class MediaTypeGuesser implements Function<byte[], String> {
  @Override
  public String apply(byte[] bytes) {
    var tika = new Tika();
    return tika.detect(bytes);
  }
}
