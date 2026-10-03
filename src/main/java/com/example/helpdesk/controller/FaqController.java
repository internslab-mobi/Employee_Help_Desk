package com.example.helpdesk.controller;

import com.example.helpdesk.dto.request.CreateFaqRequestDTO;
import com.example.helpdesk.dto.request.UpdateFaqRequestDTO;
import com.example.helpdesk.dto.response.FaqResponseDTO;
import com.example.helpdesk.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(description = "Create a new FAQ. Access: ADMIN, MANAGER")
    public ResponseEntity<FaqResponseDTO> createFaq(@Valid @RequestBody CreateFaqRequestDTO request) {
        FaqResponseDTO response = faqService.createFaq(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(description = "Update an existing FAQ. Access: ADMIN, MANAGER")
    public ResponseEntity<FaqResponseDTO> updateFaq(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFaqRequestDTO request) {
        FaqResponseDTO response = faqService.updateFaq(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(description = "Delete an FAQ. Access: ADMIN only")
    public ResponseEntity<Void> deleteFaq(@PathVariable Long id) {
        faqService.deleteFaq(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(description = "Get all active FAQs. Access: All users (including unauthenticated)")
    public ResponseEntity<List<FaqResponseDTO>> getActiveFaqs() {
        List<FaqResponseDTO> faqs = faqService.getActiveFaqs();
        return ResponseEntity.ok(faqs);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(description = "Get all FAQs (including inactive). Access: ADMIN, MANAGER")
    public ResponseEntity<List<FaqResponseDTO>> getAllFaqs() {
        List<FaqResponseDTO> faqs = faqService.getAllFaqs();
        return ResponseEntity.ok(faqs);
    }

    @GetMapping("/search")
    @PreAuthorize("permitAll()")
    @Operation(description = "Search FAQs by keyword in question or answer. Access: All users (including unauthenticated)")
    public ResponseEntity<List<FaqResponseDTO>> searchFaqs(@RequestParam String query) {
        List<FaqResponseDTO> faqs = faqService.searchFaqs(query);
        return ResponseEntity.ok(faqs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(description = "Get FAQ by ID. Access: All users (including unauthenticated)")
    public ResponseEntity<FaqResponseDTO> getFaqById(@PathVariable Long id) {
        FaqResponseDTO faq = faqService.getFaqById(id);
        return ResponseEntity.ok(faq);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(description = "Activate an FAQ. Access: ADMIN, MANAGER")
    public ResponseEntity<FaqResponseDTO> activateFaq(@PathVariable Long id) {
        FaqResponseDTO faq = faqService.activateFaq(id);
        return ResponseEntity.ok(faq);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(description = "Deactivate an FAQ. Access: ADMIN, MANAGER")
    public ResponseEntity<FaqResponseDTO> deactivateFaq(@PathVariable Long id) {
        FaqResponseDTO faq = faqService.deactivateFaq(id);
        return ResponseEntity.ok(faq);
    }
}




