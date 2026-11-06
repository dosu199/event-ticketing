package com.ticketing.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Something people buy tickets for. Maps to table "event" (V1 migration).
 *
 * <p>Status changes only through publish() and cancel(), which enforce the lifecycle:
 * <pre>
 *   DRAFT ──publish()──► PUBLISHED
 *     │                     │
 *     └──────cancel()───────┴──► CANCELLED   (final)
 * </pre>
 */
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Event() {
    }

    public Event(Venue venue, String title, String description, Instant startTime) {
        this.venue = venue;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.status = EventStatus.DRAFT;
    }

    /**
     * Puts the event on sale. Only drafts can be published, and only while the event
     * is still in the future.
     */
    public void publish(Instant now) {
        if (status != EventStatus.DRAFT) {
            throw new InvalidEventStateException(
                    "Event " + id + " is " + status + "; only DRAFT events can be published");
        }
        if (!startTime.isAfter(now)) {
            throw new InvalidEventStateException(
                    "Event " + id + " starts in the past and can't be published");
        }
        this.status = EventStatus.PUBLISHED;
    }

    /**
     * Cancels a draft or published event. Cancelling is final.
     */
    public void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new InvalidEventStateException("Event " + id + " is already cancelled");
        }
        this.status = EventStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
