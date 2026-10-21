package com.example.helpdesk.repository;

import com.example.helpdesk.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderByCreatedAtDesc();

    List<Faq> findByActiveTrueOrderByCreatedAtDesc();

    List<Faq> findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCaseOrderByCreatedAtDesc(String question, String answer);

    List<Faq> findByCategoryIdAndActiveTrueOrderByCreatedAtDesc(Long categoryId);
}



