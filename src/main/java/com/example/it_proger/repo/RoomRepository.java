package com.example.it_proger.repo;

import com.example.it_proger.models.Hotel;
import com.example.it_proger.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByHotelId(Integer hotelId);
    boolean existsByRoomNumber(String roomNumber);

    @Query("SELECT r FROM Room r WHERE " +
            "(:roomNumber IS NULL OR r.roomNumber = :roomNumber) AND " +
            "(:roomType IS NULL OR r.roomType = :roomType) AND " +
            "(:roomId IS NULL OR r.roomId = :roomId)")
    List<Room> searchRooms(@Param("roomNumber") String roomNumber,
                           @Param("roomType") String roomType,
                           @Param("roomId") Integer roomId);

    // Убираем дублирование методов с одинаковым именем
    Optional<Room> findOptionalByRoomNumber(String roomNumber); // Это метод с Optional
    Room findByRoomNumber(String roomNumber); // Это метод без Optional
    Room findByRoomNumberAndHotelId(String roomNumber, int hotelId); // Это метод без Optional
    List<Room> findAllByHotel_City_Id(int cityId);



    // Метод для получения комнат с доступностью
    List<Room> findByAvailabilityTrue();

    // Измененный метод для получения уникальных типов комнат
    @Query("SELECT DISTINCT r.roomType FROM Room r")
    List<String> findDistinctRoomType();

    // НОВЫЙ МЕТОД: Находит доступные комнаты по ID города
    @Query("SELECT r FROM Room r WHERE r.hotel.city.id = :cityId AND r.availability = true")
    List<Room> findAvailableRoomsByCityId(@Param("cityId") Integer cityId);

    Optional<Room> findByRoomNumberAndHotel(String roomNumber, Hotel hotel);
    Optional<Room> findByRoomNumberAndHotel_Id(String roomNumber, int hotelId);
}
