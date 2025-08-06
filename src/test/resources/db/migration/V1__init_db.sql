
create type job_state as enum ('SCHEDULED', 'RUNNING', 'FAILED', 'COMPLETED', 'QUEUED');

create type mjr_target_type as enum ('DIGITAL_SPECIMEN', 'MEDIA_OBJECT');

create type error_code as enum ('TIMEOUT', 'DISSCO_EXCEPTION', 'MAS_EXCEPTION');

create table mas_job_record
(
    job_id             text                     not null
        constraint mas_job_record_pk
            primary key,
    job_state          job_state                not null,
    mas_id             text                     not null,
    time_started       timestamp with time zone not null,
    time_completed     timestamp with time zone,
    annotations        jsonb,
    target_id          text                     not null,
    creator            text,
    target_type        mjr_target_type,
    batching_requested boolean,
    error              error_code,
    expires_on    timestamp with time zone not null,
    error_message text
);

create table machine_annotation_service
(
    id                     text                     not null
        primary key,
    version                integer                  not null,
    name                   varchar                  not null,
    created           timestamp with time zone not null,
    modified          timestamp with time zone not null,
    tombstoned        timestamp with time zone,
    creator                text                     not null,
    container_image        text                     not null,
    container_image_tag    text                     not null,
    creative_work_state    text,
    source_code_repository text,
    service_availability   text,
    code_maintainer        text,
    code_license           text,
    batching_permitted     boolean,
    time_to_live           integer default 86400    not null,
    data                   jsonb                    not null
);
