insert into community_detectable_object_type (type, community_authorization_id)
select 'LUMINEUX', ca.id
from community_authorization ca
where not exists (select 1
                  from community_detectable_object_type cdot
                  where cdot.community_authorization_id = ca.id
                    and cdot.type = 'LUMINEUX');
