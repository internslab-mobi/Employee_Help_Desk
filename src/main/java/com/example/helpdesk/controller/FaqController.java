package com.example.helpdesk.controller;

// Temporarily commented out until hd_faqs table is created in database
// import com.example.helpdesk.dto.request.CreateFaqRequest;
// import com.example.helpdesk.dto.request.UpdateFaqRequest;
// import com.example.helpdesk.dto.response.FaqResponse;
// import com.example.helpdesk.service.FaqService;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
//
// import java.util.List;
//
// @RestController
// @RequestMapping("/api/faqs")
// @RequiredArgsConstructor
// public class FaqController {
//
//     private final FaqService faqService;
//
//     @PostMapping
//     public ResponseEntity<FaqResponse> createFaq(
//             @Valid @RequestBody CreateFaqRequest request) {
//
//         return ResponseEntity
//                 .status(HttpStatus.CREATED)
//                 .body(faqService.createFaq(request));
//     }
//
//     @GetMapping
//     public ResponseEntity<List<FaqResponse>> getAllActiveFaqs() {
//         return ResponseEntity.ok(faqService.getAllActiveFaqs());
//     }
//
//     @GetMapping("/search")
//     public ResponseEntity<List<FaqResponse>> searchFaqs(
//             @RequestParam String keyword) {
//
//         return ResponseEntity.ok(faqService.searchFaqs(keyword));
//     }
//
//     @GetMapping("/{id}")
//     public ResponseEntity<FaqResponse> getFaqById(
//             @PathVariable Long id) {
//
//         return ResponseEntity.ok(faqService.getFaqById(id));
//     }
//
//     @PutMapping("/{id}")
//     public ResponseEntity<FaqResponse> updateFaq(
//             @PathVariable Long id,
//             @Valid @RequestBody UpdateFaqRequest request) {
//
//         return ResponseEntity.ok(faqService.updateFaq(id, request));
//     }
//
//     @DeleteMapping("/{id}")
//     public ResponseEntity<Void> deleteFaq(
//             @PathVariable Long id) {
//
//         faqService.deleteFaq(id);
//         return ResponseEntity.noContent().build();
//     }
// }
