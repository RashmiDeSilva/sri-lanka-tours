package com.example.tour_guide_system.controller;

import com.example.tour_guide_system.entity.User;
import com.example.tour_guide_system.security.CustomUserDetails;
import com.example.tour_guide_system.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @ModelAttribute User user,
            @RequestParam(value = "profilePhotoFile", required = false) MultipartFile profilePhotoFile,
            @RequestParam(value = "certificationFile", required = false) MultipartFile certificationFile,
            RedirectAttributes redirectAttributes) {

        String validationError = validateRegistration(user, profilePhotoFile, certificationFile);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/register";
        }

        String email = user.getEmail() != null ? user.getEmail().trim() : "";
        if (userService.getUserByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email address is already in use.");
            return "redirect:/register";
        }

        try {
            user.setEmail(email);
            user.setName(user.getName().trim());
            user.setPhone(user.getPhone() != null ? user.getPhone().trim() : null);
            user.setCountry(user.getCountry() != null ? user.getCountry().trim() : null);
            user.setNic(user.getNic() != null ? user.getNic().trim() : null);
            user.setGender(user.getGender() != null ? user.getGender().trim() : null);
            user.setExperience(user.getExperience() != null ? user.getExperience().trim() : null);
            user.setLanguages(user.getLanguages() != null ? user.getLanguages().trim() : null);
            user.setDescription(user.getDescription() != null ? user.getDescription().trim() : null);

            if (user.getRole() == User.Role.GUIDE) {
                user.setApproved(false);
                user.setApprovalStatus(User.ApprovalStatus.PENDING);

                if (profilePhotoFile != null && !profilePhotoFile.isEmpty()) {
                    user.setProfilePhoto(saveUploadedFile(profilePhotoFile, "photos"));
                }
                if (certificationFile != null && !certificationFile.isEmpty()) {
                    user.setCertifications(saveUploadedFile(certificationFile, "certs"));
                }
            } else {
                user.setApproved(true);
                user.setApprovalStatus(User.ApprovalStatus.APPROVED);
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setActive(true);
            user.setBlocked(false);

            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "Your account has been created successfully. Please sign in.");
            return "redirect:/login";
        } catch (DataIntegrityViolationException ex) {
            log.warn("Registration failed for {} due to data integrity issue.", email, ex);
            redirectAttributes.addFlashAttribute("error", "This account could not be created because the data already exists.");
            return "redirect:/register";
        } catch (IOException ex) {
            log.error("File upload failed during registration for {}", email, ex);
            redirectAttributes.addFlashAttribute("error", "We could not upload your files. Please try again.");
            return "redirect:/register";
        } catch (Exception ex) {
            log.error("Unexpected registration failure for {}", email, ex);
            redirectAttributes.addFlashAttribute("error", "Registration failed unexpectedly. Please try again.");
            return "redirect:/register";
        }
    }

    private String validateRegistration(User user, MultipartFile profilePhotoFile, MultipartFile certificationFile) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return "Full name is required.";
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return "Email address is required.";
        }
        if (!user.getEmail().trim().contains("@")) {
            return "Please enter a valid email address.";
        }
        if (user.getPassword() == null || user.getPassword().length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (user.getConfirmPassword() == null || !user.getPassword().equals(user.getConfirmPassword())) {
            return "Passwords do not match.";
        }
        if (user.getRole() == null) {
            return "Please select a user role.";
        }
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            return "Phone number is required.";
        }
        if (!user.getPhone().trim().matches("[0-9+\\-\\s]{7,20}")) {
            return "Please enter a valid phone number.";
        }

        if (user.getRole() == User.Role.TOURIST && (user.getCountry() == null || user.getCountry().trim().isEmpty())) {
            return "Country is required for tourist accounts.";
        }

        if (user.getRole() == User.Role.GUIDE) {
            if (user.getNic() == null || user.getNic().trim().isEmpty()) {
                return "NIC number is required for tour guide accounts.";
            }
            if (user.getDob() == null) {
                return "Date of birth is required for tour guide accounts.";
            }
            if (user.getDob().isAfter(java.time.LocalDate.now())) {
                return "Date of birth cannot be in the future.";
            }
            if (user.getGender() == null || user.getGender().trim().isEmpty()) {
                return "Gender is required for tour guide accounts.";
            }
            if (user.getHourlyRate() == null || user.getHourlyRate() <= 0) {
                return "Hourly rate must be greater than zero.";
            }
            if (user.getLanguages() == null || user.getLanguages().trim().isEmpty()) {
                return "Please select at least one language.";
            }
            if (user.getExperience() == null || user.getExperience().trim().isEmpty()) {
                return "Years of experience is required for tour guide accounts.";
            }
            if (user.getDescription() == null || user.getDescription().trim().isEmpty()) {
                return "Guide description is required.";
            }
            if (user.getDescription().length() > 1000) {
                return "Guide description is too long.";
            }
            if (profilePhotoFile == null || profilePhotoFile.isEmpty()) {
                return "Guide profile photo is required.";
            }
            if (certificationFile == null || certificationFile.isEmpty()) {
                return "Guide certification file is required.";
            }
        }

        return null;
    }

    private String saveUploadedFile(MultipartFile file, String subDir) throws IOException {
        String uploadDir = "uploads/" + subDir;
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        String originalName = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[\\\\/]+", "_").replaceAll("\\s+", "_");
        String fileName = System.currentTimeMillis() + "_" + safeName;
        Path filePath = dir.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + subDir + "/" + fileName;
    }

    @GetMapping("/defaultLoginRedirect")
    public String defaultLoginRedirect(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return "redirect:/";
        }
        User user = userDetails.getUser();
        User.Role role = user.getRole();
        Long id = user.getId();

        if (role == User.Role.ADMIN) {
            return "redirect:/home/admin/" + id;
        } else if (role == User.Role.GUIDE) {
            return "redirect:/home/tourguide/" + id;
        } else if (role == User.Role.TOURIST) {
            return "redirect:/home/tourist/" + id;
        }
        
        return "redirect:/";
    }
}
