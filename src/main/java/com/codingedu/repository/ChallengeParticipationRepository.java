package com.codingedu.repository;

import com.codingedu.entity.Challenge;
import com.codingedu.entity.ChallengeParticipation;
import com.codingedu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {
    boolean existsByUserAndChallenge(User user, Challenge challenge);
    List<ChallengeParticipation> findByUserOrderByJoinedAtDesc(User user);
    int countByChallenge(Challenge challenge);
}
