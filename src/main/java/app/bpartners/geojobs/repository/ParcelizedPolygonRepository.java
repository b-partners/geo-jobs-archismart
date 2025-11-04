package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.ParcelizedPolygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelizedPolygonRepository extends JpaRepository<ParcelizedPolygon, String> {}
