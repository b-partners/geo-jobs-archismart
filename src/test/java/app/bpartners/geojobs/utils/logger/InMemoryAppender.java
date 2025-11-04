package app.bpartners.geojobs.utils.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class InMemoryAppender extends AppenderBase<ILoggingEvent> {
  private final List<ILoggingEvent> logEvents = new ArrayList<>();

  @Override
  protected void append(ILoggingEvent eventObject) {
    logEvents.add(eventObject);
  }
}
