-- note(varchar-implicit-cast-to-jobtype)
CREATE CAST (varchar AS job_type) WITH INOUT AS IMPLICIT;