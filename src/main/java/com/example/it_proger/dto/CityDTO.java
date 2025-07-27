package com.example.it_proger.dto;

import lombok.Data;

@Data // Аннотация Lombok для генерации геттеров, сеттеров, toString и т.д.
public class CityDTO {
    private int id;
    private String name;
}