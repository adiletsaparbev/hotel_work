package com.example.it_proger.controller;

import com.example.it_proger.dto.CityDTO;
import com.example.it_proger.models.AppUser;
import com.example.it_proger.models.Room;
import com.example.it_proger.repo.AppUserRepository;
import com.example.it_proger.servise.AmenityService;
import com.example.it_proger.servise.CityService;
import com.example.it_proger.servise.CountryService;
import com.example.it_proger.servise.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final RoomService roomService;
    private final AmenityService amenityService;
    private final CountryService countryService;
    private final CityService cityService;
    private final AppUserRepository appUserRepository;

    private void addFilterAttributes(Model model) {
        List<String> amenities = amenityService.getUniqueAmenities().stream()
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        model.addAttribute("amenities", amenities);
        model.addAttribute("countries", countryService.findAllCountries());
    }

    @GetMapping("/")
    public String mainPage(Model model, Principal principal) {
        addFilterAttributes(model);
        model.addAttribute("rooms", Collections.emptyList());
        model.addAttribute("isInitialLoad", true);
        if (principal != null) {
            AppUser currentUser = appUserRepository.findByEmail(principal.getName());
            model.addAttribute("currentUser", currentUser);
        }
        return "main";
    }

    @PostMapping("/")
    public String getFilteredRooms(@RequestParam(required = false) Integer countryId, // 1. Получаем ID страны
                                   @RequestParam(required = false) Integer cityId,
                                   @RequestParam(required = false) Double minPrice,
                                   @RequestParam(required = false) Double maxPrice,
                                   @RequestParam(required = false) List<String> amenities,
                                   @RequestParam(required = false) Integer bedCount,
                                   @RequestParam(required = false) String status,
                                   Principal principal,
                                   Model model) {

        List<Room> filteredRooms = roomService.filterRooms(cityId, minPrice, maxPrice, amenities, bedCount, status);

        addFilterAttributes(model);
        model.addAttribute("rooms", filteredRooms);
        model.addAttribute("isInitialLoad", false);

        // 2. Возвращаем все полученные значения обратно в модель для сохранения состояния формы
        model.addAttribute("selectedCountryId", countryId);
        model.addAttribute("selectedCityId", cityId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedAmenities", amenities != null ? amenities : Collections.emptyList());
        model.addAttribute("selectedBedCount", bedCount);
        model.addAttribute("selectedStatus", status);

        if (principal != null) {
            AppUser currentUser = appUserRepository.findByEmail(principal.getName());
            model.addAttribute("currentUser", currentUser);
        }

        return "main";
    }

    @GetMapping("/api/cities/{countryId}")
    @ResponseBody
    public List<CityDTO> getCitiesByCountry(@PathVariable Integer countryId) {
        return cityService.findByCountryId(countryId).stream()
                .map(city -> {
                    CityDTO dto = new CityDTO();
                    dto.setId(city.getId());
                    dto.setName(city.getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
}
