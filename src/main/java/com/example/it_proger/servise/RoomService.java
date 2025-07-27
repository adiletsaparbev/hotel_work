package com.example.it_proger.servise;

import com.example.it_proger.models.Image;
import com.example.it_proger.models.Room;
import com.example.it_proger.repo.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<Room> getAvailableRooms(){
        return roomRepository.findByAvailabilityTrue();
    }

    public void saveRoom(Room room, MultipartFile file1,
                         MultipartFile file2, MultipartFile file3) throws IOException {
        // Проверка на существование комнаты с таким же номером
        if (roomRepository.findByRoomNumberAndHotel(room.getRoomNumber(), room.getHotel()).isPresent()) {
            throw new IllegalArgumentException("Room with this number already exists.");
        }

        Image image1;
        Image image2;
        Image image3;

        if (file1.getSize() != 0) {
            image1 = toImageEntity(file1);
            image1.setPreviewImage(true);
            room.addImageRoom(image1);
        }
        if (file2.getSize() != 0) {
            image2 = toImageEntity(file2);
            room.addImageRoom(image2);
        }
        if (file3.getSize() != 0) {
            image3 = toImageEntity(file3);
            room.addImageRoom(image3);
        }

        log.info("Saving new Room. Number: {}; Type: {}", room.getRoomNumber(), room.getRoomType());
        Room roomFromOb = roomRepository.save(room);
        roomFromOb.setPreviewImageId(roomFromOb.getImages().get(0).getId());
        roomRepository.save(room);
    }

    private Image toImageEntity(MultipartFile file) throws IOException{
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }

    public List<String> getRoomTypes() {
        return roomRepository.findDistinctRoomType();
    }

    // НОВЫЙ МЕТОД ДЛЯ ПОЛНОЙ ФИЛЬТРАЦИИ
    public List<Room> filterRooms(Integer cityId, Double minPrice, Double maxPrice, List<String> amenities, Integer bedCount, String status) {

        // Шаг 1: Получаем комнаты по городу. Если город не выбран, дальнейшая фильтрация невозможна.
        List<Room> filteredRooms;
        if (cityId != null) {
            // Вам нужно создать этот метод в RoomRepository
            filteredRooms = roomRepository.findAllByHotel_City_Id(cityId);

        } else {
            // Если город не выбран, возвращаем пустой список
            return List.of();
        }

        // Шаг 2: Фильтрация по минимальной цене
        if (minPrice != null) {
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getPricePerNight() >= minPrice)
                    .collect(Collectors.toList());
        }

        // Шаг 3: Фильтрация по максимальной цене
        if (maxPrice != null) {
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getPricePerNight() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // Шаг 4: Фильтрация по количеству кроватей
        if (bedCount != null) {
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getBedCount() == bedCount)
                    .collect(Collectors.toList());
        }

        // Шаг 5: Фильтрация по типу номера (статусу)
        if (status != null && !status.isEmpty()) {
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getRoomType().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        // Шаг 6: Фильтрация по удобствам (с учётом всех выбранных)
        if (amenities != null && !amenities.isEmpty()) {
            Set<String> requiredAmenities = amenities.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            filteredRooms = filteredRooms.stream()
                    .filter(room -> {
                        Set<String> roomAmenities = room.getAmenities().stream()
                                .map(amenity -> amenity.getName().toLowerCase())
                                .collect(Collectors.toSet());
                        return roomAmenities.containsAll(requiredAmenities);
                    })
                    .collect(Collectors.toList());
        }

        return filteredRooms;
    }

    // Другие методы вашего сервиса...
    // Например, метод для получения комнат по ID, который используется в RoomController
    public Room findById(int id) {
        return roomRepository.findById(id).orElse(null);
    }
}
