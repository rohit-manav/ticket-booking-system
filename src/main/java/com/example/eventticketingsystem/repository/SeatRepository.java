package com.example.eventticketingsystem.repository;

import com.example.eventticketingsystem.entity.Seat;
import com.example.eventticketingsystem.entity.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEvent_Id(Long eventId);

    List<Seat> findByEvent_IdAndStatus(Long eventId, SeatStatus status);

    List<Seat> findByEvent_IdAndCategory(Long eventId, String category);

    List<Seat> findByEvent_IdAndStatusAndCategory(Long eventId, SeatStatus status, String category);

    List<Seat> findByEvent_IdAndSeatNumberIn(Long eventId, List<String> seatNumbers);

    Optional<Seat> findByIdAndEvent_Id(Long seatId, Long eventId);
}



