package com.example.it_proger.servise;

import com.example.it_proger.models.Country;
import com.example.it_proger.repo.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {
    private final CountryRepository countryRepository;

    public List<Country> findAllCountries() {
        return countryRepository.findAll();
    }
}