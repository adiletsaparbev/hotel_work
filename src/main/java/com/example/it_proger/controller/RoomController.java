package com.example.it_proger.controller;

import com.example.it_proger.models.*;
import com.example.it_proger.repo.*;
import com.example.it_proger.servise.AppUserService;
import com.example.it_proger.servise.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final ImageRepository imageRepository;

    private final AppUserRepository userRepository;
    private final AppUserService userService;
    private final HotelRepository hotelRepository;

    // метод для добавление картинки
    private Image toImageEntity(MultipartFile file) throws IOException{
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }
    @Autowired
    private RoomRepository roomRepository;
    private final RoomService roomService;

    @Autowired
    private AmenityRepository amenityRepository;


    // вывод всех комнат
    @GetMapping
    public String getAllRooms(Model model) {
        List<Room> rooms = roomRepository.findAll();
        model.addAttribute("rooms", rooms);
        return "rooms"; // Имя шаблона HTML
    }

    // поиск комнат
    @GetMapping("/search")
    public String searchRooms(@RequestParam(required = false) String roomNumber,
                              @RequestParam(required = false) String roomType,
                              @RequestParam(required = false) Integer roomId,
                              Model model) {
        List<Room> rooms = roomRepository.searchRooms(
                roomNumber != null && !roomNumber.isEmpty() ? roomNumber : null,
                roomType != null && !roomType.isEmpty() ? roomType : null,
                roomId != null ? roomId : null
        );
        model.addAttribute("rooms", rooms);
        return "rooms"; // Укажите имя шаблона для отображения результатов
    }

//    @GetMapping("/add")
//    public String showAddRoomForm(Model model) {
//        List<Hotel> hotels = hotelRepository.findAll(); // Получаем список отелей из базы
//        model.addAttribute("hotels", hotels); // Добавляем его в модель
//        model.addAttribute("room", new Room()); // Чтобы избежать ошибок в форме
//        return "room_form"; // Название HTML-шаблона, например room_form.html
//    }

    // добавление комнат
//    @PostMapping("/add")
//    public String addRoom(@RequestParam("file1") MultipartFile file1,
//                          @RequestParam("file2") MultipartFile file2,
//                          @RequestParam("file3") MultipartFile file3,
//                          @RequestParam int hotel_id,
//                          Room room,
//                          RedirectAttributes redirectAttributes) throws IOException {
//        Optional<Hotel> hotel = hotelRepository.findById(hotel_id);
//        if (hotel.isPresent()) {
//            room.setHotel(hotel.get());
//            roomService.saveRoom(room, file1, file2, file3);
//            redirectAttributes.addFlashAttribute("message", "Комната успешно добавлена.");
//        } else {
//            redirectAttributes.addFlashAttribute("error", "Отель не найден.");
//        }
//        return "redirect:/rooms";
//    }



    // удаление комнат
    @PostMapping("/delete/{hotelId}/{id}")
    public String deleteRoom(@PathVariable Integer hotelId,
            @PathVariable int id, Model model) {
        roomRepository.deleteById(id);
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isPresent()) {
            Hotel hotel = hotelOpt.get();
            model.addAttribute("hotel", hotel);
            model.addAttribute("message", "Room deleted successfully");
            return  "redirect:/hotel-panel/info/" + hotelId;
        }
        return "main";
    }

    // подробность комнат
    @GetMapping("/room-details/{roomId}")
    public String getRoomDetails(@PathVariable("roomId") int id, Model model) {
        // Ищем комнату по ID
        Optional<Room> roomOptional = roomRepository.findById(id);

        if (roomOptional.isPresent()) {
            Room room = roomOptional.get();
            Hotel hotel = room.getHotel(); // Получаем связанный объект отеля

            // 1. Добавляем в модель ВЕСЬ ОБЪЕКТ отеля
            model.addAttribute("hotel", hotel);
            List<Amenity> amenities = amenityRepository.findByRoom(room);
            // 2. Добавляем в модель саму комнату
            model.addAttribute("room", room);
            model.addAttribute("amenities", amenities);  // Добавляем удобства в модель
            model.addAttribute("images", room.getImages());
            return "room-details";
        }
        model.addAttribute("message", "Комната с ID " + id + " не найдена.");
        return "error";
    }



    // изменение комнат

    @PostMapping("/update/{id}")
    public String updateRoom(
            @PathVariable int id,
            @ModelAttribute Room updatedRoom,
            @RequestParam(value = "file1", required = false) MultipartFile file1,
            @RequestParam(value = "file2", required = false) MultipartFile file2,
            @RequestParam(value = "file3", required = false) MultipartFile file3,
            Model model) throws IOException {

        Room room = roomRepository.findById(id).orElse(null);
        if (room != null) {
            // Обновление текстовых данных
            room.setRoomNumber(updatedRoom.getRoomNumber());
            room.setRoomType(updatedRoom.getRoomType());
            room.setBedCount(updatedRoom.getBedCount());
            room.setPricePerNight(updatedRoom.getPricePerNight());
            room.setAvailability(updatedRoom.isAvailability());
            room.setDescription(updatedRoom.getDescription());

            // Обновление изображений
            List<Image> existingImages = room.getImages();

            // Обновляем или добавляем первое изображение
            if (file1 != null && !file1.isEmpty()) {
                if (existingImages.size() > 0) {
                    Image existingImage1 = existingImages.get(0);
                    existingImage1.setName(file1.getOriginalFilename());
                    existingImage1.setContentType(file1.getContentType());
                    existingImage1.setSize(file1.getSize());
                    existingImage1.setBytes(file1.getBytes());
                } else {
                    Image newImage1 = toImageEntity(file1);
                    newImage1.setPreviewImage(true);
                    room.addImageRoom(newImage1);
                }
            }

            // Обновляем или добавляем второе изображение
            if (file2 != null && !file2.isEmpty()) {
                if (existingImages.size() > 1) {
                    Image existingImage2 = existingImages.get(1);
                    existingImage2.setName(file2.getOriginalFilename());
                    existingImage2.setContentType(file2.getContentType());
                    existingImage2.setSize(file2.getSize());
                    existingImage2.setBytes(file2.getBytes());
                } else {
                    Image newImage2 = toImageEntity(file2);
                    room.addImageRoom(newImage2);
                }
            }

            // Обновляем или добавляем третье изображение
            if (file3 != null && !file3.isEmpty()) {
                if (existingImages.size() > 2) {
                    Image existingImage3 = existingImages.get(2);
                    existingImage3.setName(file3.getOriginalFilename());
                    existingImage3.setContentType(file3.getContentType());
                    existingImage3.setSize(file3.getSize());
                    existingImage3.setBytes(file3.getBytes());
                } else {
                    Image newImage3 = toImageEntity(file3);
                    room.addImageRoom(newImage3);
                }
            }

            // Обновляем комнату в базе данных
            roomRepository.save(room);

            // Устанавливаем новое изображение для предпросмотра
            if (!room.getImages().isEmpty()) {
                room.setPreviewImageId(room.getImages().get(0).getId());
            }

            roomRepository.save(room);
            model.addAttribute("message", "Room updated successfully");
        } else {
            model.addAttribute("message", "Room not found");
        }

        return "redirect:/rooms/room-details/" + id;
    }





    // вывод удобств в странице

    @PostMapping("/{roomId}/addAmenity")
    public String addAmenityToRoom(@PathVariable int roomId, @ModelAttribute Amenity amenity, Model model) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            model.addAttribute("message", "Room not found");
            return "errorPage"; // Верните страницу ошибки, если комната не найдена
        }
        amenity.setRoom(room);
        amenityRepository.save(amenity);
        model.addAttribute("message", "Amenity added successfully");
        return "redirect:/rooms/room-details/" + roomId; // Перенаправьте на корректный маршрут
    }

    // удаление удобств
    @PostMapping("/{roomId}/deleteAmenity/{amenityId}")
    public String deleteAmenity(@PathVariable int roomId, @PathVariable Long amenityId, Model model) {
        // Находим комнату по её ID
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            // Находим удобство по ID
            Amenity amenity = amenityRepository.findById(amenityId).orElse(null);
            if (amenity != null && amenity.getRoom().getRoomId() == room.getRoomId()) {
                // Удаляем удобство, если оно связано с данной комнатой
                amenityRepository.delete(amenity);
                model.addAttribute("message", "Amenity deleted successfully");
            } else {
                model.addAttribute("message", "Amenity not found or doesn't belong to this room");
            }
        } else {
            model.addAttribute("message", "Room not found");
        }
        return "redirect:/rooms/room-details/" + roomId; // Перенаправление на страницу с деталями комнаты
    }

    // поиск удобств

    @GetMapping("/api/amenities")
    @ResponseBody
    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }


    // вход пользователя и вывод инфо
    @GetMapping("/user/{id}")
    public String getRoomUserDetails(@PathVariable int id, Model model) {
        Optional<Room> roomOptional = roomRepository.findById(id);
        if (roomOptional.isPresent()) {
            Room room = roomOptional.get();
            Hotel hotel = room.getHotel(); // Получаем связанный объект отеля

            // 1. Добавляем в модель ВЕСЬ ОБЪЕКТ отеля
            model.addAttribute("hotel", hotel);
            List<Amenity> amenities = amenityRepository.findByRoom(room); // Получаем все удобства для комнаты
            model.addAttribute("room", room);
            model.addAttribute("amenities", amenities); // Удобства
            model.addAttribute("images", room.getImages()); // Изображения
            model.addAttribute("booking", new Booking()); // Пустой объект для формы бронирования
            return "user-room"; // Шаблон с деталями комнаты
        }
        model.addAttribute("message", "Room not found");
        return "error";
    }

}
