package com.example.eventticketingsystem.repository;

import com.example.eventticketingsystem.entity.Event;
import com.example.eventticketingsystem.entity.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
}
