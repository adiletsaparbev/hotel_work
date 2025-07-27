package com.example.it_proger.controller;


import com.example.it_proger.models.AppUser;
import com.example.it_proger.models.Hotel;
import com.example.it_proger.dto.RegisterDto;
import com.example.it_proger.models.Role;
import com.example.it_proger.repo.AppUserRepository;
import com.example.it_proger.repo.HotelRepository; // <-- 1. ДОБАВЬТЕ ЭТОТ ИМПОРТ
import com.example.it_proger.servise.AppUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
public class AccountController {
    @Autowired
    private AppUserRepository repo;

    // 2. ДОБАВЬТЕ РЕПОЗИТОРИЙ ОТЕЛЕЙ
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private AppUserService userService;

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<AppUser> users = repo.findAll();
        model.addAttribute("users", users);
        return "users";
    }
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") int userId) {
        userService.deleteUser(userId);
        return "redirect:/users";
    }
    @GetMapping("/register")
    public String register(Model model) {
        RegisterDto registerDto = new RegisterDto();
        model.addAttribute(registerDto);
        model.addAttribute("success", false);
        return  "register";
    }

    @PostMapping("/register")
    public String register(
            Model model,
            @Valid @ModelAttribute RegisterDto registerDto,
            BindingResult result)
    {
        if(!registerDto.getPassword().equals(registerDto.getConfirmPassword())){
            result.addError(
                    new FieldError("registerDto", "confirmPassword", // Исправлено на confirmPassword
                            "Password and Confirm Password do not match")
            );
        }

        AppUser appUser = repo.findByEmail(registerDto.getEmail());
        if (appUser != null){
            result.addError(
                    new FieldError("registerDto", "email",
                            "Email address is already used")
            );
        }
        if (result.hasErrors()){
            return "register";
        }

        try {
            var bCryptEncoder = new BCryptPasswordEncoder();


            AppUser newUser = new AppUser();
            newUser.setFirstName(registerDto.getFirstName());
            newUser.setLastName(registerDto.getLastName());
            newUser.setEmail(registerDto.getEmail());
            newUser.setPhone(registerDto.getPhone());
            newUser.setAddress(registerDto.getAddress());
            newUser.setBirthday(registerDto.getBirthday());
            newUser.setRole(Role.USER);
            newUser.setCreatedAt(new Date());
            newUser.setPassword(bCryptEncoder.encode(registerDto.getPassword()));
            repo.save(newUser);
            model.addAttribute("registerDto", new RegisterDto());
            model.addAttribute("success", true);

        }catch (Exception ex){
            result.addError(
                    new FieldError("registerDto", "firstName",
                            ex.getMessage())
            );
        }

        return "register";

    }

    @GetMapping("/register_worker")
    public String registerWorker(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        model.addAttribute("success", false);
        return  "register_worker";
    }

    // --- ОБНОВЛЕННЫЙ МЕТОД РЕГИСТРАЦИИ РАБОТНИКА ---
    @PostMapping("/register_worker")
    public String registerWorker(
            Model model,
            @Valid @ModelAttribute RegisterDto registerDto,
            BindingResult result,
            @RequestParam(name = "hotelId", required = false) Integer hotelId) // 3. ПОЛУЧАЕМ ID ОТЕЛЯ
    {
        if(!registerDto.getPassword().equals(registerDto.getConfirmPassword())){
            result.addError(
                    new FieldError("registerDto", "confirmPassword",
                            "Password and Confirm Password do not match")
            );
        }

        AppUser appUser = repo.findByEmail(registerDto.getEmail());
        if (appUser != null){
            result.addError(
                    new FieldError("registerDto", "email",
                            "Email address is already used")
            );
        }
        if (result.hasErrors()){
            return "register_worker"; // Возвращаем правильное представление
        }

        try {
            var bCryptEncoder = new BCryptPasswordEncoder();

            AppUser newUser = new AppUser();
            newUser.setFirstName(registerDto.getFirstName());
            newUser.setLastName(registerDto.getLastName());
            newUser.setEmail(registerDto.getEmail());
            newUser.setPhone(registerDto.getPhone());
            newUser.setAddress(registerDto.getAddress());
            newUser.setRole(Role.WORKER);
            newUser.setBirthday(registerDto.getBirthday());
            newUser.setCreatedAt(new Date());
            newUser.setPassword(bCryptEncoder.encode(registerDto.getPassword()));

            // 4. ПРИВЯЗКА ОТЕЛЯ
            if (hotelId != null) {
                Optional<Hotel> hotel = hotelRepository.findById(hotelId);
                hotel.ifPresent(newUser::setHotel);
            }

            repo.save(newUser);

            model.addAttribute("registerDto", new RegisterDto());
            model.addAttribute("success", true);

        }catch (Exception ex){
            result.addError(
                    new FieldError("registerDto", "firstName",
                            ex.getMessage())
            );
        }

        return "register_worker";
    }

    // --- НОВЫЙ МЕТОД ДЛЯ ОТОБРАЖЕНИЯ СПИСКА РАБОТНИКОВ ---
    @GetMapping("/workers")
    public String listWorkers(Model model) {
        List<AppUser> workers = repo.findByRole(Role.WORKER); // Находим всех с ролью WORKER
        model.addAttribute("workers", workers);
        return "admin/workers_list";// Возвращаем новое представление
    }

    // --- НОВЫЙ МЕТОД ДЛЯ ОБНОВЛЕНИЯ ОТЕЛЯ У РАБОТНИКА ---
    @PostMapping("/workers/update-hotel")
    public String updateWorkerHotel(@RequestParam("workerId") int workerId,
                                    @RequestParam(name = "hotelId", required = false) Integer hotelId) {
        Optional<AppUser> optionalWorker = Optional.ofNullable(repo.findById(workerId));
        if (optionalWorker.isPresent()) {
            AppUser worker = optionalWorker.get();
            if (hotelId != null) {
                Optional<Hotel> optionalHotel = hotelRepository.findById(hotelId);
                // Привязываем отель, если он найден
                optionalHotel.ifPresent(worker::setHotel);
            } else {
                // Если hotelId пуст, отвязываем отель
                worker.setHotel(null);
            }
            repo.save(worker);
        }
        return "redirect:/workers";
    }

    @GetMapping("/register_admin")
    public String registerAdmin(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        model.addAttribute("success", false);
        return  "register_admin";
    }
    @PostMapping("/register_admin")
    public String registerAdmin(
            Model model,
            @Valid @ModelAttribute RegisterDto registerDto,
            BindingResult result)
    {
        if(!registerDto.getPassword().equals(registerDto.getConfirmPassword())){
            result.addError(
                    new FieldError("registerDto", "confirmPassword",
                            "Password and Confirm Password do not match")
            );
        }

        AppUser appUser = repo.findByEmail(registerDto.getEmail());
        if (appUser != null){
            result.addError(
                    new FieldError("registerDto", "email",
                            "Email address is already used")
            );
        }
        if (result.hasErrors()){
            return "register_admin";
        }

        try {
            var bCryptEncoder = new BCryptPasswordEncoder();


            AppUser newUser = new AppUser();
            newUser.setFirstName(registerDto.getFirstName());
            newUser.setLastName(registerDto.getLastName());
            newUser.setEmail(registerDto.getEmail());
            newUser.setPhone(registerDto.getPhone());
            newUser.setAddress(registerDto.getAddress());
            newUser.setBirthday(registerDto.getBirthday());
            newUser.setRole(Role.ADMIN);
            newUser.setCreatedAt(new Date());
            newUser.setPassword(bCryptEncoder.encode(registerDto.getPassword()));
            repo.save(newUser);

            model.addAttribute("registerDto", new RegisterDto());
            model.addAttribute("success", true);

        }catch (Exception ex){
            result.addError(
                    new FieldError("registerDto", "firstName",
                            ex.getMessage())
            );
        }

        return "register_admin";
    }
}