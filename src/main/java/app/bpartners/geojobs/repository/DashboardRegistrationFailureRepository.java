package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationFailure;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationStatus;
import app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRegistrationFailureRepository
    extends JpaRepository<DashboardRegistrationFailure, String> {
  Optional<DashboardRegistrationFailure> findFirstByTypeAndSubjectIdAndStatus(
      DashboardRegistrationType type, String subjectId, DashboardRegistrationStatus status);
}
