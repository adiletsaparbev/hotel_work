package com.example.it_proger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, которое выбрасывается, когда запрашиваемый отель не найден в базе данных.
 *
 * Аннотация @ResponseStatus(HttpStatus.NOT_FOUND) автоматически настраивает Spring MVC
 * так, чтобы при возникновении этого исключения клиенту возвращался
 * HTTP-статус 404 (Not Found). Это избавляет от необходимости создавать
 * специальный обработчик в @ControllerAdvice для этого конкретного случая.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class HotelNotFoundException extends RuntimeException {

    /**
     * Конструктор с сообщением об ошибке.
     * @param message Сообщение, описывающее причину ошибки.
     */
    public HotelNotFoundException(String message) {
        super(message);
    }

    /**
     * Конструктор с сообщением и причиной ошибки.
     * Полезен, если нужно обернуть другое исключение.
     * @param message Сообщение, описывающее причину ошибки.
     * @param cause Исходное исключение, которое привело к этой ошибке.
     */
    public HotelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}