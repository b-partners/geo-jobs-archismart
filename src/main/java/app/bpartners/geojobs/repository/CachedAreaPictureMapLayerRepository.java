package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.CachedAreaPictureMapLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CachedAreaPictureMapLayerRepository
    extends JpaRepository<CachedAreaPictureMapLayer, String> {}
