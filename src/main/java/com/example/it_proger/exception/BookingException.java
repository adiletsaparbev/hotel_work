package com.example.it_proger.exception;

// Простое исключение для обработки ошибок бронирования
public class BookingException extends RuntimeException {
    public BookingException(String message) {
        super(message);
    }
}