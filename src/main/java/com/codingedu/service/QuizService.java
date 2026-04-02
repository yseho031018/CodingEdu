package com.codingedu.service;

import com.codingedu.entity.*;
import com.codingedu.repository.QuizRepository;
import com.codingedu.repository.QuizResultDetailRepository;
import com.codingedu.repository.QuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizResultDetailRepository detailRepository;

    public QuizService(QuizRepository quizRepository, QuizResultRepository quizResultRepository,
                       QuizResultDetailRepository detailRepository) {
        this.quizRepository = quizRepository;
        this.quizResultRepository = quizResultRepository;
        this.detailRepository = detailRepository;
    }

    public List<Quiz> getQuizzesByDifficulty(String difficulty) {
        if ("all".equals(difficulty)) {
            return quizRepository.findAllByOrderByCreatedAtAsc();
        }
        return quizRepository.findByDifficultyOrderByCreatedAtAsc(difficulty);
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈입니다."));
    }

    public QuizResult getResultById(Long id) {
        return quizResultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결과입니다."));
    }

    public List<QuizResult> getUserResults(User user) {
        return quizResultRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public QuizResult submitQuiz(Long quizId, Map<Long, Long> userAnswers, User user) {
        Quiz quiz = getQuizById(quizId);
        int score = 0;

        QuizResult result = new QuizResult();
        result.setUser(user);
        result.setQuiz(quiz);
        result.setTotalQuestions(quiz.getQuestions().size());

        for (Question question : quiz.getQuestions()) {
            Long selectedChoiceId = userAnswers.get(question.getId());
            Choice selectedChoice = null;
            boolean correct = false;

            for (Choice choice : question.getChoices()) {
                if (choice.getId().equals(selectedChoiceId)) {
                    selectedChoice = choice;
                    if (choice.isCorrect()) { correct = true; score++; }
                    break;
                }
            }

            QuizResultDetail detail = new QuizResultDetail();
            detail.setResult(result);
            detail.setQuestion(question);
            detail.setSelectedChoice(selectedChoice);
            detail.setCorrect(correct);
            result.getDetails().add(detail);
        }

        result.setScore(score);
        return quizResultRepository.save(result);
    }

    public List<QuizResultDetail> getResultDetails(QuizResult result) {
        return detailRepository.findByResultOrderByQuestionOrderNumAsc(result);
    }

    public boolean hasData() {
        return quizRepository.count() > 0;
    }

    public long countAll() {
        return quizRepository.count();
    }

    @Transactional
    public Quiz createQuiz(String topic, String icon, String title, String description,
                           String difficulty, int timeLimit) {
        Quiz quiz = new Quiz();
        quiz.setTopic(topic);
        quiz.setIcon(icon);
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setDifficulty(difficulty);
        quiz.setTimeLimit(timeLimit);
        return quizRepository.save(quiz);
    }

    @Transactional
    public void addQuestion(Long quizId, String questionText, String explanation,
                            List<String> choiceTexts, int correctIndex) {
        Quiz quiz = getQuizById(quizId);
        Question question = new Question();
        question.setQuiz(quiz);
        question.setQuestionText(questionText);
        question.setExplanation(explanation);
        question.setOrderNum(quiz.getQuestions().size() + 1);
        for (int i = 0; i < choiceTexts.size(); i++) {
            Choice choice = new Choice();
            choice.setQuestion(question);
            choice.setChoiceText(choiceTexts.get(i));
            choice.setCorrect(i == correctIndex);
            question.getChoices().add(choice);
        }
        quiz.getQuestions().add(question);
        quizRepository.save(quiz);
    }

    @Transactional
    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }
}
