package com.codingedu.repository;

import com.codingedu.entity.QuizResult;
import com.codingedu.entity.QuizResultDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultDetailRepository extends JpaRepository<QuizResultDetail, Long> {
    List<QuizResultDetail> findByResultOrderByQuestionOrderNumAsc(QuizResult result);
}
