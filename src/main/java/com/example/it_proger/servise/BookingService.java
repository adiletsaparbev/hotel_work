package com.example.it_proger.servise;

import com.example.it_proger.exception.BookingException;
import com.example.it_proger.models.*;
import com.example.it_proger.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    // Внедряем все необходимые репозитории
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final DocumentRepository documentRepository;
    private final CardRepository cardRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository,
                          DocumentRepository documentRepository, CardRepository cardRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.documentRepository = documentRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional // Оборачиваем метод в транзакцию для целостности данных
    public Booking createBooking(int roomId, LocalDate checkInDate, LocalDate checkOutDate,
                                 double totalAmount, String cardNumber, String cvv, AppUser appUser) {

        // 1. Поиск комнаты
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BookingException("Комната не найдена"));

        // 2. Проверка паспорта
        List<Document> documents = documentRepository.findByUser(appUser);
        Document passport = documents.stream()
                .filter(doc -> "ПАСПОРТ".equalsIgnoreCase(doc.getDocumentType()))
                .findFirst()
                .orElseThrow(() -> new BookingException("У пользователя нет документа типа 'ПАСПОРТ'"));

        // 3. Проверка возраста
        Period age = Period.between(appUser.getBirthday(), LocalDate.now());
        if (age.getYears() < 18) {
            throw new BookingException("Возраст должен быть старше 18 лет");
        }

        // 4. Проверка корректности дат
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights <= 0) {
            throw new BookingException("Неправильно введены даты");
        }

        // 5. Проверка доступности комнаты
        List<Booking> existingBookings = bookingRepository.findActiveBookingsByRoomNumber(room.getRoomNumber());
        for (Booking existingBooking : existingBookings) {
            if (checkInDate.isBefore(existingBooking.getCheckOutDate()) && checkOutDate.isAfter(existingBooking.getCheckInDate())) {
                throw new BookingException("Комната уже забронирована на эти даты");
            }
        }

        // 6. Проверка суммы предоплаты
        double totalBookingCost = nights * room.getPricePerNight();
        double minimumPrepayment = totalBookingCost * 0.3;
        if (totalAmount < minimumPrepayment) {
            throw new BookingException("Предоплата должна быть больше 30% от общей суммы");
        }

        // 7. Обработка платежа
        Card card = cardRepository.findByCardNumberAndCvv(cardNumber, cvv)
                .orElseThrow(() -> new BookingException("Карта не найдена! Введите данные правильно"));

        if (card.getBalance() < totalAmount) {
            throw new BookingException("Недостаточно средств на карте");
        }

        card.setBalance(card.getBalance() - totalAmount);
        cardRepository.save(card);

        // 8. Создание и сохранение бронирования
        Hotel hotel = room.getHotel();
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setRoomNumber(room.getRoomNumber());
        booking.setFirstName(appUser.getFirstName());
        booking.setLastName(appUser.getLastName());
        booking.setDateOfBirth(appUser.getBirthday());
        booking.setPassportNumber(passport.getPassportNumber());
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkOutDate);
        booking.setTotalAmount(totalAmount);
        booking.setHotel(hotel.getNumber());
        booking.setAddress(hotel.getAddress());
        booking.setUser(appUser);
        booking.setStatus(totalAmount >= totalBookingCost ? BookingStatus.PAID : BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    // --- Остальные методы сервиса без изменений ---

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> searchByRoomNumber(String roomNumber) {
        return bookingRepository.findByRoomNumber(roomNumber);
    }

    public List<Booking> searchByDate(LocalDate date) {
        return bookingRepository.findByDate(date);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
    // НОВЫЙ, ИСПРАВЛЕННЫЙ КОД для BookingService.java

    // НОВЫЙ, ИСПРАВЛЕННЫЙ КОД для BookingService.java

    @Transactional
    public void updateBookingRoomAndAmount(Long bookingId, String newRoomNumber, double newAmount) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Бронирование не найдено"));

        int hotelId = booking.getRoom().getHotel().getId();

        Room newRoom = roomRepository.findByRoomNumberAndHotelId(newRoomNumber, hotelId);
        if (newRoom == null) {
            throw new BookingException("Комната с номером " + newRoomNumber + " не найдена в данном отеле.");
        }

        // Если пользователь выбрал ту же самую комнату, то пропускаем проверку доступности
        if (!booking.getRoom().getRoomNumber().equals(newRoomNumber)) {

            // ИСПРАВЛЕНИЕ: Используем новый метод, который ищет бронирования
            // только в пределах текущего отеля.
            List<Booking> existingBookings = bookingRepository.findActiveBookingsByRoomNumberAndHotelId(newRoomNumber, hotelId);

            for (Booking existing : existingBookings) {
                // Пропускаем проверку для текущего бронирования, которое мы изменяем
                if (existing.getId().equals(bookingId)) continue;

                // Проверка на пересечение дат
                boolean isOverlap = booking.getCheckInDate().isBefore(existing.getCheckOutDate()) &&
                        booking.getCheckOutDate().isAfter(existing.getCheckInDate());
                if (isOverlap) {
                    throw new BookingException("Комната " + newRoomNumber + " уже занята в указанные даты в этом отеле.");
                }
            }
        }

        booking.setRoom(newRoom);
        booking.setRoomNumber(newRoom.getRoomNumber());
        booking.setTotalAmount(newAmount);

        bookingRepository.save(booking);
    }


}
