CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE template
(
    id         UUID   DEFAULT gen_random_uuid() NOT NULL,
    data       character varying(255)           NOT NULL,
    status     character varying(50)            NOT NULL,
    created_at timestamptz                      NOT NULL,
    updated_at timestamptz                      NOT NULL,
    version    BIGINT DEFAULT 1                 NOT NULL,

    CONSTRAINT template_pk PRIMARY KEY (id)
);
