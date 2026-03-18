package com.codingedu.repository;

import com.codingedu.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByStatusOrderByFeaturedDescCreatedAtAsc(String status);
    boolean existsByTitle(String title);
}
