package app.bpartners.geojobs.job.repository;

import app.bpartners.geojobs.job.model.JobType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JobTypeConverter implements AttributeConverter<JobType, String> {
  @Override
  public String convertToDatabaseColumn(JobType attribute) {
    // note(varchar-implicit-cast-to-jobtype)
    return attribute == null ? null : attribute.name();
  }

  @Override
  public JobType convertToEntityAttribute(String dbData) {
    return () -> dbData;
  }
}
