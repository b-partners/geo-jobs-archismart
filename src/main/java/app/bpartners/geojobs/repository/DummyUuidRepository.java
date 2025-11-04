package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.repository.model.DummyUuid;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@PojaGenerated
@SuppressWarnings("all")
@Repository
public interface DummyUuidRepository extends JpaRepository<DummyUuid, String> {
  @Override
  List<DummyUuid> findAllById(Iterable<String> ids);
}
