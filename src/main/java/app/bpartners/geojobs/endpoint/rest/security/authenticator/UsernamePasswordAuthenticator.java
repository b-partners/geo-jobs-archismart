package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public interface UsernamePasswordAuthenticator {
  UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken);
}
