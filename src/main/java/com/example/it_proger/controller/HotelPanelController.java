package com.example.it_proger.controller;

import com.example.it_proger.exception.BookingException;
import com.example.it_proger.models.Booking;
import com.example.it_proger.models.City;
import com.example.it_proger.models.Hotel;
import com.example.it_proger.models.Room;
import com.example.it_proger.models.AppUser;
import com.example.it_proger.repo.*;
import com.example.it_proger.servise.BookingService;
import com.example.it_proger.servise.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// HotelController.java (панель работников, выбор страны, города, отеля)
@Controller
@RequestMapping("/hotel-panel")
public class HotelPanelController {
    @Autowired
    private RoomService roomService;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingService bookingService;

    @GetMapping("/locations")
    public String getCountries(Model model) {
        model.addAttribute("countries", countryRepository.findAll());
        return "worker/hotel_panel";
    }

    @GetMapping("/cities/{countryId}")
    public String getCities(@PathVariable Integer countryId, Model model) {
        List<City> cities = cityRepository.findByCountryId(countryId);
        model.addAttribute("cities", cities);
        return "fragments/cities :: cityTable";
    }

    @GetMapping("/hotels/{cityId}")
    public String getHotels(@PathVariable Integer cityId, Model model) {
        List<Hotel> hotels = hotelRepository.findByCityId(cityId);
        model.addAttribute("hotels", hotels);
        return "fragments/hotels :: hotelTable";
    }

    @GetMapping("/access")
    public String showPasswordForm(@RequestParam(required = false) Integer hotelId, Model model) {
        model.addAttribute("error", false);
        model.addAttribute("hotelId", hotelId);
        return "worker/hotel_password";
    }


    // Обработать ввод пароля
    @PostMapping("/access")
    public String checkHotelPassword(@RequestParam Integer hotelId,
                                     @RequestParam String password,
                                     Model model) {
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);

        if (hotelOpt.isPresent()) {
            Hotel hotel = hotelOpt.get();
            if (hotel.getPassword().equals(password)) {
                return "redirect:/hotel-panel/info/" + hotel.getId();
            } else {
                model.addAttribute("error", true);
                model.addAttribute("hotelId", hotelId);
                return "worker/hotel_password";
            }
        } else {
            // Отеля с таким id нет — можно вернуть ошибку или редирект
            return "redirect:/hotel-panel/locations";
        }
    }


    @GetMapping("/info/{hotelId}")
    public String hotelInfoPage(@PathVariable Integer hotelId, Model model) {
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isPresent()) {
            Hotel hotel = hotelOpt.get();
            model.addAttribute("hotel", hotel);
            model.addAttribute("rooms", hotel.getRooms());
            model.addAttribute("room", new Room()); // ← добавлено для формы
            return "worker/hotel_info";
        } else {
            return "redirect:/hotel-panel/locations";
        }
    }

    @GetMapping("/rooms/add/{hotelId}")
    public String showAddRoomForm(@PathVariable Integer hotelId, Model model) {
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isPresent()) {
            Room room = new Room();
            room.setHotel(hotelOpt.get());
            model.addAttribute("room", room);
            model.addAttribute("hotelId", hotelId);
            return "worker/add_room";
        } else {
            return "redirect:/hotel-panel/locations";
        }
    }

    @PostMapping("/rooms/add")
    public String addRoom(@RequestParam("file1") MultipartFile file1,
                          @RequestParam("file2") MultipartFile file2,
                          @RequestParam("file3") MultipartFile file3,
                          @ModelAttribute Room room, @RequestParam Integer hotelId) throws IOException {
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isPresent()) {
            room.setHotel(hotelOpt.get());
            roomService.saveRoom(room,file1,file2,file3);
        }
        return "redirect:/hotel-panel/info/" + hotelId;
    }

    @GetMapping("/rooms/search")
    public String searchRooms(
            @RequestParam Integer hotelId,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer roomId,
            Model model
    ) {
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isEmpty()) {
            return "redirect:/hotel-panel/locations";
        }

        Hotel hotel = hotelOpt.get();

        List<Room> filteredRooms = hotel.getRooms().stream()
                .filter(room -> (roomNumber == null || room.getRoomNumber().contains(roomNumber)))
                .filter(room -> (roomType == null || room.getRoomType().toLowerCase().contains(roomType.toLowerCase())))
                .filter(room -> (roomId == null || Objects.equals(room.getRoomId(), roomId)))
                .toList();

        model.addAttribute("hotel", hotel);
        model.addAttribute("rooms", filteredRooms);
        model.addAttribute("room", new Room()); // для формы создания

        return "worker/hotel_info";
    }



    @GetMapping("/bookings/{hotelId}")
    public String getHotelBookings(@PathVariable Integer hotelId, Model model) {
        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isEmpty()) {
            return "redirect:/hotel-panel/locations";
        }

        Hotel hotel = hotelOpt.get();

        // Получаем все бронирования через комнаты отеля
        List<Booking> bookings = hotel.getRooms().stream()
                .flatMap(room -> room.getBookings().stream())
                .collect(Collectors.toList());

        model.addAttribute("hotel", hotel);
        model.addAttribute("bookings", bookings);

        return "worker/hotel_bookings"; // создадим эту страницу
    }
    @GetMapping("/booking/{id}")
    public String getBookingDetails(@PathVariable Long id, Model model) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено: " + id));

        Room room = booking.getRoom();
        Hotel hotel = room.getHotel();
        AppUser user = booking.getUser();

        // ИСПРАВЛЕНИЕ: Загружаем комнаты, принадлежащие только этому отелю
        List<Room> availableRooms = roomRepository.findByHotelId(hotel.getId());

        model.addAttribute("booking", booking);
        model.addAttribute("hotel", hotel);
        model.addAttribute("room", room);
        model.addAttribute("rooms", availableRooms); // Теперь здесь будут только нужные комнаты
        model.addAttribute("user", user);

        return "worker/booking-details";
    }
//    @GetMapping("/booking/edit/{id}")
//    public String editBookingForm(@PathVariable Long id, Model model) {
//        Booking booking = bookingService.getBookingById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено: " + id));
//
//        List<Room> availableRooms = roomRepository.findAll(); // Все комнаты (можно ограничить по отелю)
//        model.addAttribute("booking", booking);
//        model.addAttribute("rooms", availableRooms);
//        return "worker/edit-booking";
//    }

    @PostMapping("/booking/update")
    public String updateBooking(@RequestParam Long bookingId,
                                @RequestParam String newRoomNumber,
                                @RequestParam double newAmount,
                                RedirectAttributes redirectAttributes) {
        try {
            bookingService.updateBookingRoomAndAmount(bookingId, newRoomNumber, newAmount);
            redirectAttributes.addFlashAttribute("message", "Бронирование успешно обновлено!");
        } catch (BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/hotel-panel/booking/" + bookingId;
    }


    @GetMapping("/new/{hotelId}")
    public String showNewBookingForm(@PathVariable("hotelId") Integer hotelId, Model model) {
        // Находим все комнаты, принадлежащие этому отелю
        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        // Передаем в модель новый пустой объект Booking для связывания с формой
        model.addAttribute("booking", new Booking());
        // Передаем список комнат для выпадающего списка в форме
        model.addAttribute("rooms", rooms);
        // Передаем ID отеля, чтобы использовать его в ссылках на странице
        model.addAttribute("hotelId", hotelId);

        // Возвращаем имя HTML-файла с формой
        return "worker/new-booking-form";
    }

    @PostMapping("/save/{hotelId}")
    public String saveBooking(@PathVariable("hotelId") Integer hotelId,
                              @ModelAttribute("booking") Booking booking,
                              @RequestParam("roomId") Integer roomId) {

        // Находим комнату по ID, который пришел из формы
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Неверный ID комнаты: " + roomId));

        // Устанавливаем связь бронирования с комнатой
        booking.setRoom(room);
        bookingRepository.save(booking);
        return "redirect:/worker/booking-chart/" + hotelId;
    }

}
