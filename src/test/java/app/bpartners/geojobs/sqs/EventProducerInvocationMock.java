package app.bpartners.geojobs.sqs;

import app.bpartners.geojobs.endpoint.event.consumer.model.ConsumableEvent;
import app.bpartners.geojobs.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import java.util.List;
import java.util.function.BiFunction;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class EventProducerInvocationMock
    implements BiFunction<LocalEventQueue, InvocationOnMock, Answer> {
  @Override
  public Answer apply(LocalEventQueue localEventQueue, InvocationOnMock invocation) {
    var consumableEvents =
        ((List) invocation.getArgument(0))
            .stream().map(argument -> toConsumableEvent((PojaEvent) argument)).toList();
    localEventQueue.handle(consumableEvents);
    return null;
  }

  ConsumableEvent toConsumableEvent(PojaEvent pojaEvent) {
    return new ConsumableEvent(
        new TypedEvent(pojaEvent.getClass().getName(), pojaEvent), () -> {}, () -> {});
  }
}
