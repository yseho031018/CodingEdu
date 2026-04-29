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

    private static final Set<String> VALID_STATUSES = Set.of("active", "upcoming", "ended");

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
                .orElseThrow(() -> new IllegalArgumentException("Challenge does not exist."));
    }

    public int getTotalParticipantCount(Challenge challenge) {
        return challenge.getParticipantCount() + participationRepository.countByChallenge(challenge);
    }

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

    public int getProgressPct(Challenge challenge) {
        if (!"active".equals(challenge.getStatus()) || challenge.getEndDate() == null) return 0;
        LocalDate today = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(challenge.getStartDate(), challenge.getEndDate());
        if (totalDays <= 0) return 100;
        long elapsed = ChronoUnit.DAYS.between(challenge.getStartDate(), today);
        elapsed = Math.max(0, Math.min(elapsed, totalDays));
        return (int) (elapsed * 100 / totalDays);
    }

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
        requireActive(challenge);
        ChallengeParticipation p = participationRepository.findByUserAndChallenge(user, challenge)
                .orElseThrow(() -> new IllegalArgumentException("Participation does not exist."));
        if (p.isCompleted()) return;
        if (githubUrl != null && !githubUrl.isBlank()) {
            String safeGithubUrl = githubUrl.trim();
            if (!safeGithubUrl.matches("^https?://github\\.com/[^/\\s]+/[^/\\s]+/?$")) {
                throw new IllegalArgumentException("Enter a valid GitHub repository URL.");
            }
            if (safeGithubUrl.length() > 300) {
                throw new IllegalArgumentException("GitHub URL is too long.");
            }
            p.setGithubUrl(safeGithubUrl);
        }
        p.setCompletedAt(java.time.LocalDateTime.now());
        participationRepository.save(p);
    }

    @Transactional
    public void join(User user, Challenge challenge) {
        requireActive(challenge);
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
        String safeTitle = requireText(title, "title", 255);
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be active, upcoming, or ended.");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required.");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate.");
        }
        if (totalTasks < 1) {
            throw new IllegalArgumentException("totalTasks must be at least 1.");
        }

        Challenge c = new Challenge();
        c.setTitle(safeTitle);
        c.setDescription(description == null ? "" : description.trim());
        c.setIcon(icon == null ? "" : icon.trim());
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
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be active, upcoming, or ended.");
        }
        Challenge c = getChallengeById(id);
        c.setStatus(status);
        challengeRepository.save(c);
    }

    @Transactional
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
    }

    private void requireActive(Challenge challenge) {
        if (!"active".equals(challenge.getStatus())) {
            throw new IllegalArgumentException("Challenge is not active.");
        }
        LocalDate today = LocalDate.now();
        if (challenge.getStartDate() != null && today.isBefore(challenge.getStartDate())) {
            throw new IllegalArgumentException("Challenge has not started.");
        }
        if (challenge.getEndDate() != null && today.isAfter(challenge.getEndDate())) {
            throw new IllegalArgumentException("Challenge has ended.");
        }
    }

    private String requireText(String value, String fieldName, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long.");
        }
        return trimmed;
    }
}
