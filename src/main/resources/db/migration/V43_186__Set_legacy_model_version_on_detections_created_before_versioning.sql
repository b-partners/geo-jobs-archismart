update "detection"
set detectable_object_model_list = (select jsonb_agg(model || '{"modelVersion": "legacy"}'::jsonb order by idx)
                                    from jsonb_array_elements(detectable_object_model_list) with ordinality as t(model, idx))
where jsonb_typeof(detectable_object_model_list) = 'array'
  and jsonb_array_length(detectable_object_model_list) > 0
  and (creation_datetime is null or creation_datetime < timestamp '2026-11-16 00:00:00');

update "detection"
set detectable_object_model = detectable_object_model || '{"modelVersion": "legacy"}'::jsonb
where jsonb_typeof(detectable_object_model) = 'object'
  and (creation_datetime is null or creation_datetime < timestamp '2026-11-16 00:00:00');

update "detection"
set detectable_object_model_list = (select jsonb_agg(case
                                                         when coalesce(model ->> 'modelVersion', '') = ''
                                                             then model || '{"modelVersion": "1.1.0"}'::jsonb
                                                         else model end order by idx)
                                    from jsonb_array_elements(detectable_object_model_list) with ordinality as t(model, idx))
where jsonb_typeof(detectable_object_model_list) = 'array'
  and jsonb_array_length(detectable_object_model_list) > 0
  and creation_datetime >= timestamp '2026-11-16 00:00:00';

update "detection"
set detectable_object_model = detectable_object_model || '{"modelVersion": "1.1.0"}'::jsonb
where jsonb_typeof(detectable_object_model) = 'object'
  and coalesce(detectable_object_model ->> 'modelVersion', '') = ''
  and creation_datetime >= timestamp '2026-11-16 00:00:00';
