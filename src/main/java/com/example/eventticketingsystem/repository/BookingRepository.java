package com.example.eventticketingsystem.repository;

import com.example.eventticketingsystem.entity.Booking;
import com.example.eventticketingsystem.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByEvent_IdAndStatus(Long eventId, BookingStatus status);

    // Customer: list my bookings — uses idx_bookings_user_id_status
    Page<Booking> findByUser_Id(Long userId, Pageable pageable);
}

