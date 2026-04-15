package com.codingedu.service;

import com.codingedu.entity.Challenge;
import com.codingedu.entity.ChallengeParticipation;
import com.codingedu.entity.User;
import com.codingedu.repository.ChallengeParticipationRepository;
import com.codingedu.repository.ChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                            ChallengeParticipationRepository participationRepository) {
        this.challengeRepository = challengeRepository;
        this.participationRepository = participationRepository;
    }

    public List<Challenge> getActiveChallenges() {
        return challengeRepository.findByStatusOrderByFeaturedDescCreatedAtAsc("active");
    }

    public List<Challenge> getUpcomingChallenges() {
        return challengeRepository.findByStatusOrderByFeaturedDescCreatedAtAsc("upcoming");
    }

    public Challenge getChallengeById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 챌린지입니다."));
    }

    // 참가자 수 = 시딩 기본값 + 실제 DB 가입자 수
    public int getTotalParticipantCount(Challenge challenge) {
        return challenge.getParticipantCount() + participationRepository.countByChallenge(challenge);
    }

    // 남은 일수 (active: endDate - today, upcoming: startDate - today)
    public long getDaysLeft(Challenge challenge) {
        LocalDate today = LocalDate.now();
        if ("active".equals(challenge.getStatus()) && challenge.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(today, challenge.getEndDate());
            return Math.max(0, days);
        }
        if ("upcoming".equals(challenge.getStatus())) {
            long days = ChronoUnit.DAYS.between(today, challenge.getStartDate());
            return Math.max(0, days);
        }
        return 0;
    }

    // 진행률 (active 챌린지: 경과일 / 총 기간)
    public int getProgressPct(Challenge challenge) {
        if (!"active".equals(challenge.getStatus()) || challenge.getEndDate() == null) return 0;
        LocalDate today = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(challenge.getStartDate(), challenge.getEndDate());
        if (totalDays <= 0) return 100;
        long elapsed = ChronoUnit.DAYS.between(challenge.getStartDate(), today);
        elapsed = Math.max(0, Math.min(elapsed, totalDays));
        return (int) (elapsed * 100 / totalDays);
    }

    // 로그인 사용자가 참가한 챌린지 ID 집합
    public Set<Long> getJoinedChallengeIds(User user) {
        return participationRepository.findByUserOrderByJoinedAtDesc(user)
                .stream()
                .map(p -> p.getChallenge().getId())
                .collect(Collectors.toSet());
    }

    public boolean isJoined(User user, Challenge challenge) {
        return participationRepository.existsByUserAndChallenge(user, challenge);
    }

    public ChallengeParticipation getParticipation(User user, Challenge challenge) {
        return participationRepository.findByUserAndChallenge(user, challenge).orElse(null);
    }

    @Transactional
    public void complete(User user, Challenge challenge, String githubUrl) {
        ChallengeParticipation p = participationRepository.findByUserAndChallenge(user, challenge)
                .orElseThrow(() -> new IllegalArgumentException("참여 정보가 없습니다."));
        if (p.isCompleted()) return;
        if (githubUrl != null && !githubUrl.isBlank()) {
            if (!githubUrl.matches("^https?://github\\.com/.+/.+")) {
                throw new IllegalArgumentException("유효한 GitHub 저장소 URL을 입력해주세요. (예: https://github.com/user/repo)");
            }
            p.setGithubUrl(githubUrl);
        }
        p.setCompletedAt(java.time.LocalDateTime.now());
        participationRepository.save(p);
    }

    @Transactional
    public void join(User user, Challenge challenge) {
        if (participationRepository.existsByUserAndChallenge(user, challenge)) return;
        ChallengeParticipation p = new ChallengeParticipation();
        p.setUser(user);
        p.setChallenge(challenge);
        participationRepository.save(p);
    }

    @Transactional
    public void leave(User user, Challenge challenge) {
        participationRepository.findByUserAndChallenge(user, challenge)
                .ifPresent(participationRepository::delete);
    }

    public List<ChallengeParticipation> getJoinedParticipations(User user) {
        return participationRepository.findByUserOrderByJoinedAtDesc(user);
    }

    public List<Challenge> getAllChallenges() {
        return challengeRepository.findAll(org.springframework.data.domain.Sort.by("id").ascending());
    }

    public long countAll() {
        return challengeRepository.count();
    }

    @Transactional
    public Challenge createChallenge(String title, String description, String icon,
                                     String status, boolean featured,
                                     LocalDate startDate, LocalDate endDate,
                                     int totalTasks) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("챌린지 제목은 필수입니다.");
        if (!java.util.Set.of("active", "upcoming", "ended").contains(status))
            throw new IllegalArgumentException("상태는 active/upcoming/ended 중 하나여야 합니다.");
        if (startDate == null) throw new IllegalArgumentException("시작일은 필수입니다.");
        if (totalTasks < 1) throw new IllegalArgumentException("총 과제 수는 1 이상이어야 합니다.");

        Challenge c = new Challenge();
        c.setTitle(title);
        c.setDescription(description);
        c.setIcon(icon);
        c.setStatus(status);
        c.setFeatured(featured);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setTotalTasks(totalTasks);
        c.setParticipantCount(0);
        return challengeRepository.save(c);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        if (!java.util.Set.of("active", "upcoming", "ended").contains(status))
            throw new IllegalArgumentException("상태는 active/upcoming/ended 중 하나여야 합니다.");
        Challenge c = getChallengeById(id);
        c.setStatus(status);
        challengeRepository.save(c);
    }

    @Transactional
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
    }
}
