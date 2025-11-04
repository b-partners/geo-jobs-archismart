package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.endpoint.event.model.UuidCreated;
import app.bpartners.geojobs.repository.DummyUuidRepository;
import app.bpartners.geojobs.repository.model.DummyUuid;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@PojaGenerated
@SuppressWarnings("all")
@Service
@AllArgsConstructor
@Slf4j
public class UuidCreatedService implements Consumer<UuidCreated> {

  private final DummyUuidRepository dummyUuidRepository;

  @Override
  public void accept(UuidCreated uuidCreated) {
    var dummyUuid = new DummyUuid();
    dummyUuid.setId(uuidCreated.getUuid());
    dummyUuidRepository.save(dummyUuid);
  }
}
