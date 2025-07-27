package com.example.it_proger.servise;

import com.example.it_proger.models.Booking;
import com.example.it_proger.models.Room;
import com.example.it_proger.repo.BookingRepository;
import com.example.it_proger.repo.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingChartService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

//    public Map<Integer, List<TimelineItem>> prepareTimelineData(int hotelId, YearMonth yearMonth) {
//        List<Room> rooms = roomRepository.findByHotelId(hotelId);
//        LocalDate startOfMonth = yearMonth.atDay(1);
//        LocalDate endOfMonth = yearMonth.atEndOfMonth();
//
//        // Захватываем брони, которые пересекаются с месяцем
//        List<Booking> bookings = bookingRepository.findBookingsByHotelAndDateRange(hotelId, startOfMonth.minusMonths(1), endOfMonth.plusMonths(1));
//
//        Map<Integer, List<Booking>> bookingsByRoom = bookings.stream()
//                .collect(Collectors.groupingBy(b -> b.getRoom().getRoomId()));
//
//        Map<Integer, List<TimelineItem>> timelineData = new HashMap<>();
//
//        for (Room room : rooms) {
//            List<TimelineItem> timeline = new ArrayList<>();
//            List<Booking> roomBookings = bookingsByRoom.getOrDefault(room.getRoomId(), List.of());
//
//            int day = 1;
//            while (day <= yearMonth.lengthOfMonth()) {
//                LocalDate currentDate = yearMonth.atDay(day);
//                Booking bookingForCurrentDay = findBookingForDate(roomBookings, currentDate);
//
//                if (bookingForCurrentDay != null) {
//                    LocalDate segmentStart = bookingForCurrentDay.getCheckInDate().isBefore(startOfMonth) ? startOfMonth : bookingForCurrentDay.getCheckInDate();
//
//                    // Мы добавляем блок брони, только если текущий день является началом видимого сегмента
//                    if (currentDate.equals(segmentStart)) {
//                        LocalDate segmentEnd = bookingForCurrentDay.getCheckOutDate().isAfter(endOfMonth.plusDays(1)) ? endOfMonth.plusDays(1) : bookingForCurrentDay.getCheckOutDate();
//                        int span = (int) ChronoUnit.DAYS.between(segmentStart, segmentEnd);
//
//                        if (span > 0) {
//                            timeline.add(new BookingTimelineItem(bookingForCurrentDay, span));
//                            day += span; // Перескакиваем через все дни этой брони
//                        } else { // Если бронь нулевой длины (ошибка данных?), просто пропускаем день
//                            timeline.add(new EmptyTimelineItem());
//                            day++;
//                        }
//                    } else { // Этот день занят, но он не начало видимого сегмента. Пропускаем.
//                        timeline.add(new EmptyTimelineItem());
//                        day++;
//                    }
//                } else { // День свободен
//                    timeline.add(new EmptyTimelineItem());
//                    day++;
//                }
//            }
//            timelineData.put(room.getRoomId(), timeline);
//        }
//        return timelineData;
//    }

    private Booking findBookingForDate(List<Booking> bookings, LocalDate date) {
        for (Booking booking : bookings) {
            if (!date.isBefore(booking.getCheckInDate()) && date.isBefore(booking.getCheckOutDate())) {
                return booking;
            }
        }
        return null;
    }
}
