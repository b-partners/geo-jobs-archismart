package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionAddressConversionJobRepository
    extends JpaRepository<DetectionAddressConversionJob, String> {}
