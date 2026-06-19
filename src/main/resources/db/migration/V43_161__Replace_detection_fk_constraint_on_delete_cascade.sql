-- Table feature_delimitation_computing
alter table feature_delimitation_computing
    drop constraint if exists feature_delimitation_computing_detection_identifier_fkey;

alter table feature_delimitation_computing
    add constraint feature_delimitation_computing_detection_identifier_fkey foreign key (detection_identifier) references detection (id) on delete cascade;

-- Table detection_feature
alter table detection_feature
    drop constraint if exists detection_feature_id_detection_fkey;

alter table detection_feature
    add constraint detection_feature_id_detection_fkey foreign key (id_detection) references detection (id) on delete cascade;
