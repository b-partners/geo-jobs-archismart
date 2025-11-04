package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.conf.FacadeIT;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("TODO")
public class HikariIT extends FacadeIT {

  @Autowired private HikariDataSource hikariDataSource;

  @Test
  void connection_pool_can_be_closed() {
    hikariDataSource.close();
  }
}
