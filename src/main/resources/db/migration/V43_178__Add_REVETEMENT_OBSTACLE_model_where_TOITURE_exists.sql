update community_authorization
set detectable_models = array_append(detectable_models, 'REVETEMENT_OBSTACLE')
where 'TOITURE' = any (detectable_models)
  and not ('REVETEMENT_OBSTACLE' = any (detectable_models));
