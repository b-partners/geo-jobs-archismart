package app.bpartners.geojobs.endpoint.rest.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;

@Configuration
@AllArgsConstructor
public class AuthConf {
  private final AuthProvider authProvider;

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(authProvider);
  }
}
