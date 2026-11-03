CREATE TABLE venue (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    city        VARCHAR(255) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id    BIGINT NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    start_time  TIMESTAMPTZ NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_event_venue FOREIGN KEY (venue_id) REFERENCES venue(id) ON DELETE RESTRICT,
    CONSTRAINT chk_event_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

CREATE TABLE seat (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id    BIGINT NOT NULL,
    row_label   VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    category    VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    price       NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_seat_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT chk_seat_number CHECK (seat_number > 0),
    CONSTRAINT chk_seat_category CHECK (category IN ('STANDARD', 'VIP')),
    CONSTRAINT chk_seat_price CHECK (price >= 0),
    CONSTRAINT uq_seat_event_row_number UNIQUE (event_id, row_label, seat_number)
);

CREATE INDEX idx_event_venue_id ON event (venue_id);