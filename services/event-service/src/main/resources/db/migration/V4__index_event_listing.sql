-- Indexes for the paged event list (ORDER BY start_time, id).
-- Filtered by status:  WHERE status = ? ORDER BY start_time, id
CREATE INDEX idx_event_status_start_time ON event (status, start_time, id);
-- Unfiltered:          ORDER BY start_time, id
CREATE INDEX idx_event_start_time ON event (start_time, id);
