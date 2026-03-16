package app.bpartners.geojobs.service.google.maps;

import org.springframework.stereotype.Component;

@Component
public class AddressValidator {

  public void accept(String address) {
    if (address.length() < 3 || address.length() > 200) {
      throw new IllegalStateException("Address to search must be between 3 and 200 chars");
    }
  }
}
