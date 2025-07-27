package com.example.it_proger.servise;

import com.example.it_proger.models.City;
import com.example.it_proger.repo.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {
    private final CityRepository cityRepository;

    public List<City> findByCountryId(Integer countryId) {
        if (countryId == null) {
            return Collections.emptyList();
        }
        return cityRepository.findByCountryId(countryId);
    }
}