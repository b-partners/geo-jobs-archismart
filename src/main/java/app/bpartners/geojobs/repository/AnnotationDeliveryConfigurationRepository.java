package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnotationDeliveryConfigurationRepository
    extends JpaRepository<AnnotationDeliveryConfiguration, String> {

  @Query(
      value =
          "select deliveryConfiguration.* from annotation_delivery_configuration"
              + " deliveryConfiguration order by creation_datetime desc limit 1",
      nativeQuery = true)
  Optional<AnnotationDeliveryConfiguration> findLatestConfiguration();
}
