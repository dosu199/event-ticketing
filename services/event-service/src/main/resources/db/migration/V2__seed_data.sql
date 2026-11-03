-- Demo data for local development: 2 venues, 3 events, seats for 2 events.

INSERT INTO venue (name, city, address) VALUES
    ('Budapest Park',  'Budapest', 'Soroksári út 60, 1095 Budapest'),
    ('Zetra Olympic Hall', 'Sarajevo', 'Alipašina bb, 71000 Sarajevo');

INSERT INTO event (venue_id, title, description, start_time, status)
SELECT v.id, 'Summer Rock Night', 'Open-air rock concert with three bands.',
       now() + interval '30 days', 'PUBLISHED'
FROM venue v WHERE v.name = 'Budapest Park';

INSERT INTO event (venue_id, title, description, start_time, status)
SELECT v.id, 'Tech Conference 2027', 'Two days of talks on cloud and microservices.',
       now() + interval '90 days', 'DRAFT'
FROM venue v WHERE v.name = 'Zetra Olympic Hall';

INSERT INTO event (venue_id, title, description, start_time, status)
SELECT v.id, 'Winter Jazz Evening', 'Cancelled due to artist illness.',
       now() + interval '60 days', 'CANCELLED'
FROM venue v WHERE v.name = 'Budapest Park';

-- Summer Rock Night: rows A-B are VIP (89.99), rows C-D standard (39.99), 10 seats each
INSERT INTO seat (event_id, row_label, seat_number, category, price)
SELECT e.id, r.row_label, n.seat_number,
       CASE WHEN r.row_label IN ('A', 'B') THEN 'VIP' ELSE 'STANDARD' END,
       CASE WHEN r.row_label IN ('A', 'B') THEN 89.99 ELSE 39.99 END
FROM event e
CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D')) AS r(row_label)
CROSS JOIN generate_series(1, 10) AS n(seat_number)
WHERE e.title = 'Summer Rock Night';

-- Tech Conference 2027: rows A-C, 8 seats each, all standard
INSERT INTO seat (event_id, row_label, seat_number, category, price)
SELECT e.id, r.row_label, n.seat_number, 'STANDARD', 149.00
FROM event e
CROSS JOIN (VALUES ('A'), ('B'), ('C')) AS r(row_label)
CROSS JOIN generate_series(1, 8) AS n(seat_number)
WHERE e.title = 'Tech Conference 2027';