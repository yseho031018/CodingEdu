package com.codingedu.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_notes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lang", "lesson_idx"}))
public class LessonNote {

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public int getLessonIdx() { return lessonIdx; }
    public void setLessonIdx(int lessonIdx) { this.lessonIdx = lessonIdx; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
