# Update all protocol metadata that are "updated" to dtype = 1
update mr_protocol_metadata set dtype = 2 where mr_protocol_metadata.id in (select distinct updated_metadata_id from mr_protocol where updated_metadata_id is not NULL);