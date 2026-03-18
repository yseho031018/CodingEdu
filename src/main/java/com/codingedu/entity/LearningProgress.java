package com.codingedu.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lang", "lesson_idx"}))
public class LearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String lang;

    @Column(name = "lesson_idx", nullable = false)
    private int lessonIdx;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public int getLessonIdx() { return lessonIdx; }
    public void setLessonIdx(int lessonIdx) { this.lessonIdx = lessonIdx; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
