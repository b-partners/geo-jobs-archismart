package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.feature.FeatureDelimitationComputing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureDelimitationComputingRepository
    extends JpaRepository<FeatureDelimitationComputing, String> {}
