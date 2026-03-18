package com.codingedu.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_courses")
public class LessonCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String lang;           // html, css, javascript, ...

    @Column(nullable = false, length = 100)
    private String title;          // "WEB1 - HTML"

    @Column(nullable = false, length = 10)
    private String icon;           // "🌐"

    @Column(nullable = false, length = 20)
    private String category;       // web, java, c, mobile, etc

    @Column(nullable = false, length = 20)
    private String level;          // beginner, intermediate, advanced

    @Column(nullable = false)
    private int lessonCount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getLessonCount() { return lessonCount; }
    public void setLessonCount(int lessonCount) { this.lessonCount = lessonCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
