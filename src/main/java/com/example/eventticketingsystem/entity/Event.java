package com.example.eventticketingsystem.entity;

import com.example.eventticketingsystem.entity.enums.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "events",
        indexes = {
                @Index(name = "idx_events_status", columnList = "status"),
                @Index(name = "idx_events_event_date_time", columnList = "event_date_time")
        }
)
@SQLRestriction("deleted = false")
public class Event extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 300)
    private String venue;

    @Column(name = "event_date_time", nullable = false)
    private Instant eventDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.INACTIVE;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private Set<Seat> seats = new HashSet<>();

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private Set<Booking> bookings = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Instant getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(Instant eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Set<Seat> getSeats() {
        return seats;
    }

    public Set<Booking> getBookings() {
        return bookings;
    }
}

