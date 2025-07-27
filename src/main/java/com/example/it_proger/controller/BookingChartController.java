package com.example.it_proger.controller;

import com.example.it_proger.models.Booking;
import com.example.it_proger.models.BookingStatus;
import com.example.it_proger.models.Hotel;
import com.example.it_proger.models.Room;
import com.example.it_proger.repo.BookingRepository;
import com.example.it_proger.repo.HotelRepository;
import com.example.it_proger.repo.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/worker")
@RequiredArgsConstructor
public class BookingChartController {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    @GetMapping("/booking-chart/{hotelId}")
    public String showBookingChart(@PathVariable("hotelId") int hotelId,
                                   @RequestParam(required = false) Integer year,
                                   @RequestParam(required = false) Integer month,
                                   Model model) {

        YearMonth currentYearMonth;
        if (year != null && month != null) {
            currentYearMonth = YearMonth.of(year, month);
        } else {
            currentYearMonth = YearMonth.from(LocalDate.now());
        }

        List<Room> rooms = roomRepository.findByHotelId(hotelId);

        LocalDate startDate = currentYearMonth.atDay(1);
        LocalDate endDate = currentYearMonth.atEndOfMonth().plusDays(1);

        List<Booking> bookings = bookingRepository.findBookingsByHotelAndDateRange(hotelId, startDate, endDate);

        // ✅ Проверка оплаты для каждого бронирования
        for (Booking booking : bookings) {
            LocalDate checkIn = booking.getCheckInDate();
            LocalDate checkOut = booking.getCheckOutDate();
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            double totalBookingCost = nights * booking.getRoom().getPricePerNight();

            if (booking.getTotalAmount() >= totalBookingCost) {
                booking.setStatus(BookingStatus.PAID);
            } else {
                booking.setStatus(BookingStatus.PENDING);
            }
        }

        // ✅ Отправка данных в шаблон
        model.addAttribute("hotelId", hotelId);
        model.addAttribute("rooms", rooms);
        model.addAttribute("bookings", bookings);
        model.addAttribute("yearMonth", currentYearMonth);
        model.addAttribute("daysInMonth", currentYearMonth.lengthOfMonth());

        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow();
        model.addAttribute("hotel", hotel);
        model.addAttribute("currentDate", LocalDate.now());
        model.addAttribute("previousMonth", currentYearMonth.minusMonths(1));
        model.addAttribute("nextMonth", currentYearMonth.plusMonths(1));

        String monthName = currentYearMonth.getMonth()
                .getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
        model.addAttribute("monthName", monthName);

        // ✅ Добавляем сводку по статусам комнат
        addRoomStatusSummary(model, hotelId, rooms);

        return "worker/booking-chart";
    }


    /**
     * Рассчитывает сводку по статусам номеров на СЕГОДНЯ, определяет статус для каждого номера
     * и добавляет все в модель.
     */
    private void addRoomStatusSummary(Model model, int hotelId, List<Room> rooms) {
        LocalDate today = LocalDate.now();
        List<Booking> allHotelBookings = bookingRepository.findAllByRoom_Hotel_Id(hotelId);

        // Расчет счетчиков (оставляем без изменений, они работают верно)
        long checkingInCount = allHotelBookings.stream()
                .filter(b -> b.getCheckInDate().isEqual(today))
                .count();

        long checkingOutCount = allHotelBookings.stream()
                .filter(b -> b.getCheckOutDate().isEqual(today))
                .count();

        long occupiedCount = allHotelBookings.stream()
                .filter(b -> b.getCheckInDate().isBefore(today) && b.getCheckOutDate().isAfter(today))
                .count();

        long totalOccupiedTonight = allHotelBookings.stream()
                .filter(b -> !b.getCheckInDate().isAfter(today) && b.getCheckOutDate().isAfter(today))
                .count();

        long availableCount = rooms.size() - totalOccupiedTonight;

        model.addAttribute("checkingInCount", checkingInCount);
        model.addAttribute("checkingOutCount", checkingOutCount);
        model.addAttribute("occupiedCount", occupiedCount);
        model.addAttribute("availableCount", availableCount);

        // ИСПРАВЛЕННАЯ ЛОГИКА: Создаем карту статусов для каждого номера
        Map<Integer, String> roomStatuses = new HashMap<>();
        for (Booking b : allHotelBookings) {
            Integer roomId = b.getRoom().getRoomId();

            // Логика такая:
            // 1. "Заселяется" - самый высокий приоритет, если заезд сегодня.
            // 2. "Выселяется" - второй приоритет, если выезд сегодня.
            // 3. "Занят" - если гость уже живет и не выезжает сегодня.

            // Статус "Занят" или "Заселяется"
            // Гость уже живет (заехал вчера или раньше) ИЛИ заезжает сегодня,
            // и при этом выезжает завтра или позже.
            if (!b.getCheckInDate().isAfter(today) && b.getCheckOutDate().isAfter(today)) {
                if (b.getCheckInDate().isEqual(today)) {
                    roomStatuses.put(roomId, "checking-in"); // Заезд сегодня -> "Заселяется"
                } else {
                    roomStatuses.put(roomId, "occupied"); // Уже живет -> "Занят"
                }
            }

            // Статус "Выселяется"
            // Эта проверка выполняется отдельно.
            // Если выезд сегодня, ставим статус "Выселяется", но только если там еще не стоит "Заселяется"
            // (на случай, если в тот же день один гость выехал, а другой заехал).
            if (b.getCheckOutDate().isEqual(today)) {
                roomStatuses.putIfAbsent(roomId, "checking-out");
            }
        }
        model.addAttribute("roomStatuses", roomStatuses);
    }


    public static Booking getBookingForRoomOnDate(List<Booking> bookings, int roomId, LocalDate date) {
        if (bookings == null || date == null) {
            return null;
        }
        for (Booking booking : bookings) {
            if (booking.getRoom().getRoomId() == roomId) {
                if (!date.isBefore(booking.getCheckInDate()) && date.isBefore(booking.getCheckOutDate())) {
                    return booking;
                }
            }
        }
        return null;
    }
}
