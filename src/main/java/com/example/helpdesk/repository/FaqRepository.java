package com.example.helpdesk.repository;

// Temporarily commented out until hd_faqs table is created in database
// import com.example.helpdesk.entity.Faq;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import org.springframework.stereotype.Repository;
//
// import java.util.List;
//
// @Repository
// public interface FaqRepository extends JpaRepository<Faq, Long> {
//
//     List<Faq> findByActiveTrueOrderByCreatedAtDesc();
//
//     @Query("SELECT f FROM Faq f WHERE f.active = true AND (LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%')))")
//     List<Faq> searchActiveFaqs(@Param("keyword") String keyword);
//
//     List<Faq> findByCategoryIdAndActiveTrueOrderByCreatedAtDesc(Long categoryId);
// }
