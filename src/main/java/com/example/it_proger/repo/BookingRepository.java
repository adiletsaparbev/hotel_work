package com.example.it_proger.repo;

import com.example.it_proger.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Поиск по номеру комнаты
    List<Booking> findByRoomNumber(String roomNumber);

    // Поиск по дате (вхождение даты в диапазон бронирования)
    @Query("SELECT b FROM Booking b WHERE :date BETWEEN b.checkInDate AND b.checkOutDate")
    List<Booking> findByDate(LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.roomNumber = :roomNumber " +
            "AND b.checkOutDate >= :checkInDate " +
            "AND b.checkInDate <= :checkOutDate")
    List<Booking> findActiveBookingsByRoom(@Param("roomNumber") String roomNumber,
                                           @Param("checkInDate") LocalDate checkInDate,
                                           @Param("checkOutDate") LocalDate checkOutDate);
    @Query("SELECT b FROM Booking b WHERE b.roomNumber = :roomNumber AND b.status != 'CANCELLED'")
    List<Booking> findActiveBookingsByRoomNumber(@Param("roomNumber") String roomNumber);

    @Query("SELECT b FROM Booking b WHERE b.room.hotel.id = :hotelId " +
            "AND b.checkInDate < :endDate " +
            "AND b.checkOutDate > :startDate")
    List<Booking> findBookingsByHotelAndDateRange(@Param("hotelId") int hotelId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b JOIN b.room r WHERE r.hotel.id = :hotelId " +
            "AND b.checkInDate <= :endDate AND b.checkOutDate > :startDate")
    List<Booking> findBookingsByHotelAndDateRangeOverlap(
            @Param("hotelId") int hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    // Добавьте этот метод в интерфейс BookingRepository.java

    @Query("SELECT b FROM Booking b WHERE b.room.hotel.id = :hotelId AND b.roomNumber = :roomNumber AND (b.status = com.example.it_proger.models.BookingStatus.PAID OR b.status = com.example.it_proger.models.BookingStatus.PENDING)")
    List<Booking> findActiveBookingsByRoomNumberAndHotelId(@Param("roomNumber") String roomNumber, @Param("hotelId") int hotelId);

     List<Booking> findAllByRoom_Hotel_Id(int hotelId);

}
