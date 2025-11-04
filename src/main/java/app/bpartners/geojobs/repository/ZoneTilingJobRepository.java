package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneTilingJobRepository extends JobRepository<ZoneTilingJob> {}
