package app.bpartners.geojobs.conf;

import app.bpartners.geojobs.PojaGenerated;
import java.time.Duration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

@PojaGenerated
@SuppressWarnings("all")
public class PostgresConf {

  private final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:13.9")
          .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

  void start() {
    postgres.start();
  }

  void stop() {
    postgres.stop();
  }

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }
}
