package com.formation.step1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/step1")
public class HelloController {

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello World!");
    }

    @GetMapping("/greet")
    public ResponseEntity<String> greet(
            @RequestParam(defaultValue = "Monde")
            @NotBlank(message = "Le nom ne peut pas être vide")
            @Size(min = 2, max = 50, message = "Le nom doit faire entre 2 et 50 caractères")
            String nom) {
        return ResponseEntity.ok("Bonjour " + nom + "!");
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getInfo() {
        ApiResponse response = new ApiResponse(
            "Application de formation",
            "v1.0",
            "Spring Boot + PostgreSQL"
        );
        return ResponseEntity.ok(response);
    }

    static class ApiResponse {
        public String title;
        public String version;
        public String description;

        public ApiResponse(String title, String version, String description) {
            this.title = title;
            this.version = version;
            this.description = description;
        }
    }

}
