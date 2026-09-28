package com.example.helpdesk.service.impl;

import com.example.helpdesk.dto.request.CreateFaqRequest;
import com.example.helpdesk.dto.request.UpdateFaqRequest;
import com.example.helpdesk.dto.response.FaqResponse;
import com.example.helpdesk.entity.Category;
import com.example.helpdesk.entity.Faq;
import com.example.helpdesk.repository.CategoryRepository;
import com.example.helpdesk.repository.FaqRepository;
import com.example.helpdesk.service.FaqService;
import com.example.helpdesk.util.AuthenticatedEmployeeUtil;
import com.example.helpdesk.util.TimezoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticatedEmployeeUtil authenticatedEmployeeUtil;

    @Override
    @Transactional
    public FaqResponse createFaq(CreateFaqRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
        }

        Faq faq = Faq.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .category(category)
                .active(true)
                .build();

        Faq savedFaq = faqRepository.save(faq);
        log.info("Created FAQ with id: {}", savedFaq.getId());

        return toResponse(savedFaq);
    }

    @Override
    @Transactional
    public FaqResponse updateFaq(Long id, UpdateFaqRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
        }

        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setCategory(category);
        if (request.getActive() != null) {
            faq.setActive(request.getActive());
        }

        Faq savedFaq = faqRepository.save(faq);
        log.info("Updated FAQ with id: {}", id);

        return toResponseWithUpdatedAt(savedFaq);
    }

    @Override
    @Transactional
    public void deleteFaq(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new IllegalArgumentException("FAQ not found: " + id);
        }
        faqRepository.deleteById(id);
        log.info("Deleted FAQ with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> getActiveFaqs() {
        return faqRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> getAllFaqs() {
        return faqRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> searchFaqs(String query) {
        return faqRepository.findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCaseOrderByCreatedAtDesc(
                        query, query)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FaqResponse getFaqById(Long id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
        return toResponse(faq);
    }

    @Override
    @Transactional
    public FaqResponse activateFaq(Long id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
        faq.setActive(true);
        Faq savedFaq = faqRepository.save(faq);
        log.info("Activated FAQ with id: {}", id);
        return toResponseWithUpdatedAt(savedFaq);
    }

    @Override
    @Transactional
    public FaqResponse deactivateFaq(Long id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ not found: " + id));
        faq.setActive(false);
        Faq savedFaq = faqRepository.save(faq);
        log.info("Deactivated FAQ with id: {}", id);
        return toResponseWithUpdatedAt(savedFaq);
    }

    private FaqResponse toResponse(Faq faq) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();
        return FaqResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .categoryId(faq.getCategory() != null ? faq.getCategory().getId() : null)
                .categoryName(faq.getCategory() != null ? faq.getCategory().getName() : null)
                .active(faq.getActive())
                .createdAt(TimezoneUtil.toOffsetDateTime(faq.getCreatedAt(), timezone))
                .build();
    }

    private FaqResponse toResponseWithUpdatedAt(Faq faq) {
        String timezone = authenticatedEmployeeUtil.getAuthenticatedEmployeeTimezone();
        return FaqResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .categoryId(faq.getCategory() != null ? faq.getCategory().getId() : null)
                .categoryName(faq.getCategory() != null ? faq.getCategory().getName() : null)
                .active(faq.getActive())
                .createdAt(TimezoneUtil.toOffsetDateTime(faq.getCreatedAt(), timezone))
                .updatedAt(TimezoneUtil.toOffsetDateTime(faq.getUpdatedAt(), timezone))
                .build();
    }
}
