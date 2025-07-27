package com.example.it_proger.controller;
import com.example.it_proger.models.AppUser;
import com.example.it_proger.models.Document;
import com.example.it_proger.repo.AppUserRepository;
import com.example.it_proger.repo.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/document")
public class DocumentController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    // Показать форму добавления документа по userId
    @GetMapping("/add/{userId}")
    public String showAddDocumentForm(@PathVariable("userId") int userId, Model model) {
        AppUser user = userRepository.findById(userId);
        if (user == null) {
            model.addAttribute("error", "Пользователь не найден.");
            return "error";
        }
        model.addAttribute("user", user);
        return "profile/add-document";  // это имя Thymeleaf шаблона (add-document.html)
    }
    @PostMapping("/document/add")
    public String addDocument(@RequestParam("documentType") String documentType,
                              @RequestParam("passportSeries") String passportSeries,
                              @RequestParam("passportNumber") String passportNumber,
                              @RequestParam("issuedBy") String issuedBy,
                              @RequestParam("issueDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
                              @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                              @RequestParam("placeOfBirth") String placeOfBirth,
                              @RequestParam("nationality") String nationality,
                              Principal principal,
                              Model model) {

        AppUser user = userRepository.findByEmail(principal.getName());
        if (user == null) {
            model.addAttribute("message", "Пользователь не найден");
            return "add-document";
        }

        Document document = new Document();
        document.setUser(user);
        document.setDocumentType(documentType);
        document.setPassportSeries(passportSeries);
        document.setPassportNumber(passportNumber);
        document.setIssuedBy(issuedBy);
        document.setIssueDate(issueDate);
        document.setExpiryDate(expiryDate);
        document.setPlaceOfBirth(placeOfBirth);
        document.setNationality(nationality);

        documentRepository.save(document);

        model.addAttribute("message", "Документ успешно сохранён!");
        return "redirect:/profile";
    }
    @GetMapping("/edit/{id}")
    public String showEditDocumentForm(@PathVariable("id") int documentId, Model model, Principal principal) {
        // Находим документ по ID
        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isEmpty()) {
            // Если документ не найден, возвращаем ошибку
            return "error";
        }
        Document document = optionalDocument.get();

        // Проверяем, что текущий пользователь является владельцем этого документа
        String currentUserEmail = principal.getName();
        if (!document.getUser().getEmail().equals(currentUserEmail)) {
            // Если кто-то пытается редактировать чужой документ - запрещаем
            return "error"; // Страница "Доступ запрещен"
        }

        // Передаем найденный документ в модель
        model.addAttribute("document", document);

        // Открываем страницу редактирования
        return "profile/edit-document";
    }

    /**
     * POST-метод для сохранения измененных данных документа.
     */
    @PostMapping("/edit/{id}")
    public String updateDocument(@PathVariable("id") int documentId,
                                 @ModelAttribute("document") Document updatedDocumentData,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {

        // Находим существующий документ в базе
        Optional<Document> optionalDocument = documentRepository.findById(documentId);
        if (optionalDocument.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Документ не найден.");
            return "redirect:/profile";
        }
        Document existingDocument = optionalDocument.get();

        // Проверяем права доступа (повторная проверка безопасности)
        String currentUserEmail = principal.getName();
        if (!existingDocument.getUser().getEmail().equals(currentUserEmail)) {
            redirectAttributes.addFlashAttribute("error", "Доступ запрещен.");
            return "redirect:/profile";
        }

        // Обновляем поля существующего документа данными из формы
        existingDocument.setDocumentType(updatedDocumentData.getDocumentType());
        existingDocument.setPassportSeries(updatedDocumentData.getPassportSeries());
        existingDocument.setPassportNumber(updatedDocumentData.getPassportNumber());
        existingDocument.setIssuedBy(updatedDocumentData.getIssuedBy());
        existingDocument.setIssueDate(updatedDocumentData.getIssueDate());
        existingDocument.setExpiryDate(updatedDocumentData.getExpiryDate());
        existingDocument.setPlaceOfBirth(updatedDocumentData.getPlaceOfBirth());
        existingDocument.setNationality(updatedDocumentData.getNationality());

        // Сохраняем обновленный документ
        documentRepository.save(existingDocument);

        // Добавляем сообщение об успехе и перенаправляем в профиль
        redirectAttributes.addFlashAttribute("success", "Данные документа успешно обновлены!");
        return "redirect:/profile";
    }
}
