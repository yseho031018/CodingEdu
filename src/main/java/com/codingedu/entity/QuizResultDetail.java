package com.codingedu.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_result_details")
public class QuizResultDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private QuizResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // 사용자가 선택한 선택지 (미응답이면 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_choice_id")
    private Choice selectedChoice;

    @Column(nullable = false)
    private boolean correct;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QuizResult getResult() { return result; }
    public void setResult(QuizResult result) { this.result = result; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
    public Choice getSelectedChoice() { return selectedChoice; }
    public void setSelectedChoice(Choice selectedChoice) { this.selectedChoice = selectedChoice; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
}
