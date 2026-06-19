package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.detection.DetectionFileObject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionFileObjectRepository extends JpaRepository<DetectionFileObject, String> {}
