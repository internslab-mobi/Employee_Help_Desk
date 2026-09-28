package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.CreateFaqRequest;
import com.example.helpdesk.dto.request.UpdateFaqRequest;
import com.example.helpdesk.dto.response.FaqResponse;

import java.util.List;

public interface FaqService {

    FaqResponse createFaq(CreateFaqRequest request);

    FaqResponse updateFaq(Long id, UpdateFaqRequest request);

    void deleteFaq(Long id);

    List<FaqResponse> getActiveFaqs();

    List<FaqResponse> getAllFaqs();

    List<FaqResponse> searchFaqs(String query);

    FaqResponse getFaqById(Long id);

    FaqResponse activateFaq(Long id);

    FaqResponse deactivateFaq(Long id);
}
