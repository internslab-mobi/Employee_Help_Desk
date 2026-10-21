package com.example.helpdesk.service;

import com.example.helpdesk.dto.request.CreateFaqRequestDTO;
import com.example.helpdesk.dto.request.UpdateFaqRequestDTO;
import com.example.helpdesk.dto.response.FaqResponseDTO;

import java.util.List;

public interface FaqService {

    FaqResponseDTO createFaq(CreateFaqRequestDTO request);

    FaqResponseDTO updateFaq(Long id, UpdateFaqRequestDTO request);

    void deleteFaq(Long id);

    List<FaqResponseDTO> getActiveFaqs();

    List<FaqResponseDTO> getAllFaqs();

    List<FaqResponseDTO> searchFaqs(String query);

    FaqResponseDTO getFaqById(Long id);

    FaqResponseDTO activateFaq(Long id);

    FaqResponseDTO deactivateFaq(Long id);
}




