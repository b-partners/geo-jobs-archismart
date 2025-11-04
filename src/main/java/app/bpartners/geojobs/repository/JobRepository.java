package app.bpartners.geojobs.repository;

import app.bpartners.geojobs.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JobRepository<J extends Job> extends JpaRepository<J, String> {}
