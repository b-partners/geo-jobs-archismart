-- annotation_delivery_job
CREATE INDEX IF NOT EXISTS annotation_delivery_job_detection_job_id_idx ON "annotation_delivery_job"(detection_job_id);

-- annotation_retrieving_job
CREATE INDEX IF NOT EXISTS annotation_retrieving_job_detection_job_id_idx ON "annotation_retrieving_job"(detection_job_id);
CREATE INDEX IF NOT EXISTS annotation_retrieving_job_annotation_job_id_idx ON "annotation_retrieving_job"(annotation_job_id);

-- community_authorization
CREATE INDEX IF NOT EXISTS community_authorization_email_idx ON "community_authorization"(email);
CREATE INDEX IF NOT EXISTS community_authorization_dashboard_api_key_idx ON "community_authorization"(dashboard_api_key);

-- community_used_surface
CREATE INDEX IF NOT EXISTS community_used_surface_usage_datetime_idx ON "community_used_surface"(usage_datetime);
CREATE INDEX IF NOT EXISTS community_used_surface_community_authorization_id_idx ON "community_used_surface"(community_authorization_id);

-- detectable_object_configuration
CREATE INDEX IF NOT EXISTS detectable_object_configuration_detection_job_id_idx ON "detectable_object_configuration"(detection_job_id);

-- detection
CREATE INDEX IF NOT EXISTS detection_creation_datetime_idx ON "detection"(creation_datetime);
CREATE INDEX IF NOT EXISTS detection_ztj_id_idx ON "detection"(ztj_id);
CREATE INDEX IF NOT EXISTS detection_zdj_id_idx ON "detection"(zdj_id);
CREATE INDEX IF NOT EXISTS detection_community_owner_id_idx ON "detection"(community_owner_id);
CREATE INDEX IF NOT EXISTS detection_end_to_end_id_idx ON "detection"(end_to_end_id);

-- detection_step
CREATE INDEX IF NOT EXISTS detection_step_detection_id_idx ON "detection_step"(detection_id);

-- task_statistic
CREATE INDEX IF NOT EXISTS task_statistic_updated_at_idx ON "task_statistic"(updated_at);
CREATE INDEX IF NOT EXISTS task_statistic_job_id_idx ON "task_statistic"(job_id);