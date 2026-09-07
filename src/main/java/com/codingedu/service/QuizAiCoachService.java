package com.codingedu.service;

import com.codingedu.entity.Choice;
import com.codingedu.entity.Quiz;
import com.codingedu.entity.QuizResult;
import com.codingedu.entity.QuizResultDetail;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuizAiCoachService {

    private final GeminiAiService geminiAiService;
    private final ObjectMapper objectMapper;

    public QuizAiCoachService(GeminiAiService geminiAiService, ObjectMapper objectMapper) {
        this.geminiAiService = geminiAiService;
        this.objectMapper = objectMapper;
    }

    public AiQuizFeedback analyze(Quiz quiz, QuizResult result, List<QuizResultDetail> details) {
        int total = Math.max(1, result.getTotalQuestions());
        int wrongCount = (int) details.stream().filter(detail -> !detail.isCorrect()).count();
        int percentage = result.getScore() * 100 / total;
        List<WrongReview> wrongReviews = buildWrongReviews(details);

        String summary = buildSummary(quiz, percentage, wrongCount);
        String focusConcept = buildFocusConcept(quiz, wrongReviews);
        String nextAction = buildNextAction(percentage, wrongCount, focusConcept);
        List<String> studyTips = buildStudyTips(percentage, wrongReviews);

        AiQuizFeedback fallback = new AiQuizFeedback(summary, focusConcept, nextAction, studyTips, wrongReviews);
        return generateGeminiFeedback(quiz, result, percentage, fallback).orElse(fallback);
    }

    private List<WrongReview> buildWrongReviews(List<QuizResultDetail> details) {
        List<WrongReview> reviews = new ArrayList<>();
        int wrongOrder = 1;

        for (QuizResultDetail detail : details) {
            if (detail.isCorrect()) continue;

            Choice correctChoice = detail.getQuestion().getChoices().stream()
                    .filter(Choice::isCorrect)
                    .findFirst()
                    .orElse(null);
            String selected = detail.getSelectedChoice() != null
                    ? detail.getSelectedChoice().getChoiceText()
                    : "선택하지 않음";
            String correct = correctChoice != null ? correctChoice.getChoiceText() : "정답 정보 없음";
            String explanation = detail.getQuestion().getExplanation();
            if (explanation == null || explanation.isBlank()) {
                explanation = "문제의 핵심 개념과 선택지 차이를 다시 비교해 보세요.";
            }

            reviews.add(new WrongReview(
                    wrongOrder++,
                    detail.getQuestion().getQuestionText(),
                    selected,
                    correct,
                    explanation
            ));
        }

        return reviews;
    }

    private String buildSummary(Quiz quiz, int percentage, int wrongCount) {
        String topic = quiz.getTopic() != null ? quiz.getTopic() : "이번 주제";
        if (wrongCount == 0) {
            return topic + " 개념을 안정적으로 이해하고 있어요. 다음 단계 문제로 넘어가도 좋습니다.";
        }
        if (percentage >= 70) {
            return topic + "의 큰 흐름은 잡혀 있습니다. 틀린 문제의 선택지 차이만 정리하면 점수가 더 올라갈 수 있어요.";
        }
        if (percentage >= 50) {
            return topic + "의 기본 개념은 일부 이해했지만, 문제에서 요구하는 조건을 놓친 부분이 보여요.";
        }
        return topic + "의 핵심 용어와 기본 동작 방식을 먼저 다시 확인하는 것이 좋습니다.";
    }

    private String buildFocusConcept(Quiz quiz, List<WrongReview> wrongReviews) {
        String text = (quiz.getTitle() + " " + quiz.getDescription() + " " + quiz.getTopic() + " "
                + wrongReviews.stream().map(WrongReview::questionText).reduce("", (a, b) -> a + " " + b))
                .toLowerCase();

        if (text.contains("dom")) return "DOM 선택과 이벤트 처리";
        if (text.contains("function") || text.contains("함수")) return "함수 선언과 실행 흐름";
        if (text.contains("variable") || text.contains("변수") || text.contains("let") || text.contains("const")) return "변수와 스코프";
        if (text.contains("array") || text.contains("배열")) return "배열과 반복 처리";
        if (text.contains("css") || text.contains("selector") || text.contains("선택자")) return "CSS 선택자와 스타일 적용 우선순위";
        if (text.contains("html") || text.contains("tag") || text.contains("태그")) return "HTML 태그 구조와 의미";
        if (text.contains("spring")) return "Spring MVC 요청 처리 흐름";
        if (text.contains("sql") || text.contains("db")) return "데이터 조회 조건과 테이블 관계";
        return quiz.getTopic() != null && !quiz.getTopic().isBlank() ? quiz.getTopic() + " 핵심 개념" : "문제 조건 읽기";
    }

    private String buildNextAction(int percentage, int wrongCount, String focusConcept) {
        if (wrongCount == 0) {
            return "오답이 없으니 같은 주제의 중급 문제나 챌린지로 넘어가 보세요.";
        }
        if (percentage >= 70) {
            return focusConcept + "에서 틀린 문제만 다시 풀고, 비슷한 선택지를 비교해 보세요.";
        }
        return focusConcept + "을 10분 정도 복습한 뒤 같은 퀴즈를 다시 도전해 보세요.";
    }

    private List<String> buildStudyTips(int percentage, List<WrongReview> wrongReviews) {
        List<String> tips = new ArrayList<>();
        if (wrongReviews.isEmpty()) {
            tips.add("정답을 맞힌 이유를 말로 설명해 보면 장기 기억에 더 잘 남습니다.");
            tips.add("다음에는 같은 주제의 난이도를 한 단계 높여 도전해 보세요.");
            return tips;
        }

        tips.add("오답 문제는 정답만 외우지 말고, 내가 고른 선택지가 왜 아닌지 한 줄로 적어보세요.");
        tips.add("문제에서 요구한 조건과 선택지의 표현이 어떻게 다른지 먼저 비교해 보세요.");
        if (percentage < 70) {
            tips.add("개념 복습 후 바로 재도전하면 어떤 부분이 개선됐는지 확인하기 쉽습니다.");
        }
        return tips;
    }

    private Optional<AiQuizFeedback> generateGeminiFeedback(Quiz quiz, QuizResult result,
                                                           int percentage, AiQuizFeedback fallback) {
        String systemInstruction = """
                당신은 초보 개발자를 돕는 한국어 코딩 학습 코치입니다.
                사용자의 퀴즈 결과를 바탕으로 친절하고 짧게 오답을 설명하세요.
                반드시 JSON만 반환하고, 마크다운 코드블록은 사용하지 마세요.
                """;

        String prompt = buildGeminiQuizPrompt(quiz, result, percentage, fallback);
        return geminiAiService.generateText(systemInstruction, prompt)
                .flatMap(text -> parseGeminiFeedback(text, fallback));
    }

    private String buildGeminiQuizPrompt(Quiz quiz, QuizResult result, int percentage, AiQuizFeedback fallback) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("아래 퀴즈 결과를 분석해서 JSON 형식으로만 답변해 주세요.\n")
                .append("JSON schema:\n")
                .append("{\"summary\":\"한 문장 결과 분석\",\"focusConcept\":\"집중 개념\",")
                .append("\"nextAction\":\"다음 학습 행동\",\"studyTips\":[\"팁1\",\"팁2\"],")
                .append("\"wrongReviews\":[{\"order\":1,\"explanation\":\"해당 오답 설명\"}]}\n\n")
                .append("퀴즈 제목: ").append(nullToBlank(quiz.getTitle())).append('\n')
                .append("주제: ").append(nullToBlank(quiz.getTopic())).append('\n')
                .append("설명: ").append(nullToBlank(quiz.getDescription())).append('\n')
                .append("점수: ").append(result.getScore()).append(" / ").append(result.getTotalQuestions())
                .append(" (").append(percentage).append("%)\n\n")
                .append("오답 목록:\n");

        if (fallback.wrongReviews().isEmpty()) {
            prompt.append("- 오답 없음\n");
            return prompt.toString();
        }

        fallback.wrongReviews().stream().limit(5).forEach(review -> prompt
                .append(review.order()).append(". 문제: ").append(review.questionText()).append('\n')
                .append("   선택한 답: ").append(review.selectedAnswer()).append('\n')
                .append("   정답: ").append(review.correctAnswer()).append('\n')
                .append("   기존 해설: ").append(review.explanation()).append('\n'));

        return prompt.toString();
    }

    private Optional<AiQuizFeedback> parseGeminiFeedback(String text, AiQuizFeedback fallback) {
        try {
            GeminiQuizFeedback parsed = objectMapper.readValue(stripJsonFence(text), GeminiQuizFeedback.class);
            String summary = cleanOrDefault(parsed.summary(), fallback.summary());
            String focusConcept = cleanOrDefault(parsed.focusConcept(), fallback.focusConcept());
            String nextAction = cleanOrDefault(parsed.nextAction(), fallback.nextAction());
            List<String> studyTips = sanitizeTips(parsed.studyTips(), fallback.studyTips());
            List<WrongReview> wrongReviews = mergeWrongReviews(fallback.wrongReviews(), parsed.wrongReviews());
            return Optional.of(new AiQuizFeedback(summary, focusConcept, nextAction, studyTips, wrongReviews));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    private List<String> sanitizeTips(List<String> tips, List<String> fallback) {
        if (tips == null || tips.isEmpty()) {
            return fallback;
        }
        List<String> cleaned = tips.stream()
                .map(tip -> cleanOrDefault(tip, ""))
                .filter(tip -> !tip.isBlank())
                .limit(3)
                .toList();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private List<WrongReview> mergeWrongReviews(List<WrongReview> fallback, List<GeminiWrongReview> generated) {
        if (generated == null || generated.isEmpty()) {
            return fallback;
        }
        Map<Integer, String> explanationByOrder = generated.stream()
                .filter(review -> review.order() > 0 && review.explanation() != null && !review.explanation().isBlank())
                .collect(Collectors.toMap(
                        GeminiWrongReview::order,
                        review -> cleanOrDefault(review.explanation(), ""),
                        (first, ignored) -> first
                ));

        return fallback.stream()
                .map(review -> new WrongReview(
                        review.order(),
                        review.questionText(),
                        review.selectedAnswer(),
                        review.correctAnswer(),
                        cleanOrDefault(explanationByOrder.get(review.order()), review.explanation())
                ))
                .toList();
    }

    private String stripJsonFence(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private String cleanOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 320 ? cleaned.substring(0, 320) : cleaned;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    public record AiQuizFeedback(
            String summary,
            String focusConcept,
            String nextAction,
            List<String> studyTips,
            List<WrongReview> wrongReviews
    ) {}

    public record WrongReview(
            int order,
            String questionText,
            String selectedAnswer,
            String correctAnswer,
            String explanation
    ) {}

    private record GeminiQuizFeedback(
            String summary,
            String focusConcept,
            String nextAction,
            List<String> studyTips,
            List<GeminiWrongReview> wrongReviews
    ) {}

    private record GeminiWrongReview(
            int order,
            String explanation
    ) {}
}

