package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.repository.model.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, String> {}
