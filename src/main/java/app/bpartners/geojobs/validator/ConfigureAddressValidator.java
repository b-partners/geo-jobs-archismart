package app.bpartners.geojobs.validator;

import app.bpartners.geojobs.endpoint.rest.model.Address;
import app.bpartners.geojobs.model.exception.BadRequestException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ConfigureAddressValidator implements Consumer<List<Address>> {

  public void accept(List<Address> addresses) {
    if (addresses == null || addresses.isEmpty()) {
      throw new BadRequestException("Addresses is mandatory");
    }
    if (addresses.stream().anyMatch(Objects::isNull)) {
      throw new BadRequestException("Provided address inside list can not be null");
    }
  }
}
