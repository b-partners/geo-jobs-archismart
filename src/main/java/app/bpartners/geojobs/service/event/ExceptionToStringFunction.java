package app.bpartners.geojobs.service.event;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ExceptionToStringFunction implements Function<Exception, String> {
  @Override
  public String apply(Exception e) {
    var sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return e.getMessage() + ", stackTrace=" + sw;
  }
}
