package com.example.it_proger.controller;

import com.example.it_proger.exception.BookingException;
import com.example.it_proger.models.*;
import com.example.it_proger.repo.AppUserRepository;
import com.example.it_proger.repo.RoomRepository;
import com.example.it_proger.servise.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/book")
@RequiredArgsConstructor // Используем конструктор Lombok для внедрения зависимостей
public class BookingController {

    private final BookingService bookingService;
    private final AppUserRepository appUserRepository;
    private final RoomRepository roomRepository; // Нужен для отображения формы

    @GetMapping("/bookings")
    public String showAllBookings(Model model) {
        List<Booking> bookings = bookingService.getAllBookings(); //
        model.addAttribute("bookings", bookings);
        return "bookings";
    }

    @GetMapping("/search")
    public String searchBookings(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        List<Booking> results;

        if (roomNumber != null && !roomNumber.isEmpty()) {
            results = bookingService.searchByRoomNumber(roomNumber); //
        } else if (date != null) {
            results = bookingService.searchByDate(date); //
        } else {
            results = bookingService.getAllBookings(); //
        }
        model.addAttribute("bookings", results);
        return "bookings";
    }

    @GetMapping("/add-booking/{roomId}")
    public String showBookingForm(@PathVariable int roomId, Model model) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            model.addAttribute("message", "Комната не найдена");
            return "error";
        }
        model.addAttribute("room", room);
        model.addAttribute("booking", new Booking());
        return "add-booking";
    }

    @PostMapping("/add-booking")
    public String bookRoom(@RequestParam("roomId") int roomId,
                           @RequestParam("checkInDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
                           @RequestParam("checkOutDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
                           @RequestParam("totalAmount") double totalAmount,
                           @RequestParam String cardNumber,
                           @RequestParam String cvv,
                           Principal principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        AppUser appUser = appUserRepository.findByEmail(principal.getName());

        try {
            // Вся логика теперь в одной строке вызова сервиса
            Booking newBooking = bookingService.createBooking(roomId, checkInDate, checkOutDate, totalAmount, cardNumber, cvv, appUser);

            String statusMessage = newBooking.getStatus() == BookingStatus.PAID ? "PAID" : "PENDING";
            redirectAttributes.addFlashAttribute("message", "Оплата прошла успешно! Статус бронирования: " + statusMessage + ".");
            return "redirect:/profile";

        } catch (BookingException e) {
            // Если сервис выбросил ошибку, ловим ее и показываем сообщение на форме
            model.addAttribute("message", e.getMessage());
            // Нужно снова добавить комнату в модель, чтобы форма отобразилась корректно
            Room room = roomRepository.findById(roomId).orElse(null);
            model.addAttribute("room", room);
            model.addAttribute("booking", new Booking()); // Добавляем пустой объект для формы
            return "add-booking";
        }
    }
}
