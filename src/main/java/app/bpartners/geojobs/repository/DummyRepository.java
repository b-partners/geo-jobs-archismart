package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.PojaGenerated;
import app.bpartners.geojobs.repository.model.Dummy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@PojaGenerated
@SuppressWarnings("all")
@Repository
public interface DummyRepository extends JpaRepository<Dummy, String> {

  @Override
  List<Dummy> findAll();
}
