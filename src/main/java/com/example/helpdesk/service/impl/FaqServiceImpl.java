package com.example.helpdesk.service.impl;

// Temporarily commented out until hd_faqs table is created in database
// import com.example.helpdesk.dto.request.CreateFaqRequest;
// import com.example.helpdesk.dto.request.UpdateFaqRequest;
// import com.example.helpdesk.dto.response.FaqResponse;
// import com.example.helpdesk.entity.Category;
// import com.example.helpdesk.entity.Faq;
// import com.example.helpdesk.repository.CategoryRepository;
// import com.example.helpdesk.repository.FaqRepository;
// import com.example.helpdesk.service.FaqService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
//
// import java.util.List;
// import java.util.stream.Collectors;
//
// @Service
// @RequiredArgsConstructor
// @Slf4j
// @Transactional
// public class FaqServiceImpl implements FaqService {
//
//     private final FaqRepository faqRepository;
//     private final CategoryRepository categoryRepository;
//
//     @Override
//     public FaqResponse createFaq(CreateFaqRequest request) {
//         Category category = null;
//         if (request.getCategoryId() != null) {
//             category = categoryRepository.findById(request.getCategoryId())
//                     .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
//         }
//
//         Faq faq = Faq.builder()
//                 .question(request.getQuestion())
//                 .answer(request.getAnswer())
//                 .category(category)
//                 .active(true)
//                 .build();
//
//         faq = faqRepository.save(faq);
//
//         log.info("FAQ created with id {}", faq.getId());
//
//         return toResponse(faq);
//     }
//
//     @Override
//     @Transactional(readOnly = true)
//     public List<FaqResponse> getAllActiveFaqs() {
//         return faqRepository.findByActiveTrueOrderByCreatedAtDesc()
//                 .stream()
//                 .map(this::toResponse)
//                 .collect(Collectors.toList());
//     }
//
//     @Override
//     @Transactional(readOnly = true)
//     public List<FaqResponse> searchFaqs(String keyword) {
//         return faqRepository.searchActiveFaqs(keyword)
//                 .stream()
//                 .map(this::toResponse)
//                 .collect(Collectors.toList());
//     }
//
//     @Override
//     @Transactional(readOnly = true)
//     public FaqResponse getFaqById(Long id) {
//         Faq faq = faqRepository.findById(id)
//                 .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
//         return toResponse(faq);
//     }
//
//     @Override
//     public FaqResponse updateFaq(Long id, UpdateFaqRequest request) {
//         Faq faq = faqRepository.findById(id)
//                 .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
//
//         if (request.getCategoryId() != null) {
//             Category category = categoryRepository.findById(request.getCategoryId())
//                     .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
//             faq.setCategory(category);
//         }
//
//         faq.setQuestion(request.getQuestion());
//         faq.setAnswer(request.getAnswer());
//
//         if (request.getActive() != null) {
//             faq.setActive(request.getActive());
//         }
//
//         faq = faqRepository.save(faq);
//
//         log.info("FAQ updated with id {}", id);
//
//         return toResponse(faq);
//     }
//
//     @Override
//     public void deleteFaq(Long id) {
//         Faq faq = faqRepository.findById(id)
//                 .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
//
//         faqRepository.delete(faq);
//
//         log.info("FAQ deleted with id {}", id);
//     }
//
//     private FaqResponse toResponse(Faq faq) {
//         return FaqResponse.builder()
//                 .id(faq.getId())
//                 .question(faq.getQuestion())
//                 .answer(faq.getAnswer())
//                 .categoryId(faq.getCategory() != null ? faq.getCategory().getId() : null)
//                 .categoryName(faq.getCategory() != null ? faq.getCategory().getName() : null)
//                 .active(faq.getActive())
//                 .createdAt(faq.getCreatedAt())
//                 .updatedAt(faq.getUpdatedAt())
//                 .build();
//     }
// }
