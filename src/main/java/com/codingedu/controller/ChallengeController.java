package com.codingedu.controller;

import com.codingedu.entity.Challenge;
import com.codingedu.entity.User;
import com.codingedu.service.ChallengeService;
import com.codingedu.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserService userService;

    public ChallengeController(ChallengeService challengeService, UserService userService) {
        this.challengeService = challengeService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Challenge> activeChallenges = challengeService.getActiveChallenges();
        List<Challenge> upcomingChallenges = challengeService.getUpcomingChallenges();

        Set<Long> joinedIds = null;
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            joinedIds = challengeService.getJoinedChallengeIds(user);
        }

        // 각 챌린지의 참가자 수, 남은 일수, 진행률을 Map으로 전달
        java.util.Map<Long, Integer> participantCounts = new java.util.HashMap<>();
        java.util.Map<Long, Long> daysLeftMap = new java.util.HashMap<>();
        java.util.Map<Long, Integer> progressPctMap = new java.util.HashMap<>();

        for (Challenge c : activeChallenges) {
            participantCounts.put(c.getId(), challengeService.getTotalParticipantCount(c));
            daysLeftMap.put(c.getId(), challengeService.getDaysLeft(c));
            progressPctMap.put(c.getId(), challengeService.getProgressPct(c));
        }
        for (Challenge c : upcomingChallenges) {
            daysLeftMap.put(c.getId(), challengeService.getDaysLeft(c));
        }

        model.addAttribute("activeChallenges", activeChallenges);
        model.addAttribute("upcomingChallenges", upcomingChallenges);
        model.addAttribute("joinedIds", joinedIds);
        model.addAttribute("participantCounts", participantCounts);
        model.addAttribute("daysLeftMap", daysLeftMap);
        model.addAttribute("progressPctMap", progressPctMap);

        return "challenge";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        Challenge challenge = challengeService.getChallengeById(id);
        boolean joined = false;
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            joined = challengeService.isJoined(user, challenge);
        }

        model.addAttribute("challenge", challenge);
        model.addAttribute("joined", joined);
        model.addAttribute("participantCount", challengeService.getTotalParticipantCount(challenge));
        model.addAttribute("daysLeft", challengeService.getDaysLeft(challenge));
        model.addAttribute("progressPct", challengeService.getProgressPct(challenge));

        return "challenge-detail";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        User user = userService.findByUsername(userDetails.getUsername());
        Challenge challenge = challengeService.getChallengeById(id);
        challengeService.join(user, challenge);
        return "redirect:/challenge/" + id;
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        User user = userService.findByUsername(userDetails.getUsername());
        Challenge challenge = challengeService.getChallengeById(id);
        challengeService.leave(user, challenge);
        return "redirect:/challenge/" + id;
    }
}
