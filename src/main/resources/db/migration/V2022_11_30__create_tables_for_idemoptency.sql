CREATE TABLE idempotency_phase_result
(
    id         VARCHAR(125)   NOT NULL
        CONSTRAINT idempotency_phase_result_pk PRIMARY KEY,
    value      TEXT           NOT NULL,
    created_at TIMESTAMP      DEFAULT current_timestamp
);

CREATE TABLE idempotency_phase_lock
(
    id         VARCHAR(125)   NOT NULL
        CONSTRAINT idempotency_phase_lock_pk PRIMARY KEY,
    expires    TIMESTAMP      NOT NULL,
    created_at TIMESTAMP      DEFAULT current_timestamp
);