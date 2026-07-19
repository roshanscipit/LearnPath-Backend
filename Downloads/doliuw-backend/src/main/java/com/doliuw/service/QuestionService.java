package com.doliuw.service;

import com.doliuw.dto.QuestionDtos.*;
import com.doliuw.entity.Question;
import com.doliuw.exception.AppException;
import com.doliuw.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionRepository questionRepository;

    // Max questions allowed per session
    private static final int MIN_Q = 5;
    private static final int MAX_Q = 10;

    // ── Public: get shuffled quiz session ─────────────────────────

    public QuizSessionDto getQuizSession(QuizSessionRequest req) {
        int count = Math.max(MIN_Q, Math.min(MAX_Q, req.getCount()));

        List<Question> questions;

        if ("MIXED".equalsIgnoreCase(req.getQuestionType())) {
            // Mix coding + aptitude + system design
            int each = Math.max(1, count / 3);
            List<Question> coding = questionRepository.findRandomByType("CODING", each);
            List<Question> aptitude = questionRepository.findRandomByType("APTITUDE", each);
            List<Question> sysDesign = questionRepository.findRandomByType("SYSTEM_DESIGN", count - 2 * each);
            questions = new ArrayList<>();
            questions.addAll(coding);
            questions.addAll(aptitude);
            questions.addAll(sysDesign);
            Collections.shuffle(questions); // final shuffle for mixed
        } else if (req.getDifficulty() != null && !req.getDifficulty().isBlank()) {
            questions = questionRepository.findRandomByTypeAndDifficulty(
                req.getQuestionType().toUpperCase(), capitalize(req.getDifficulty()), count);
        } else if (req.getCompanyTag() != null && !req.getCompanyTag().isBlank()) {
            questions = questionRepository.findRandomForCompany(req.getCompanyTag(), count);
        } else {
            questions = questionRepository.findRandomByType(req.getQuestionType().toUpperCase(), count);
        }

        if (questions.isEmpty()) {
            throw new AppException("No questions found for the requested criteria", HttpStatus.NOT_FOUND);
        }

        QuizSessionDto session = new QuizSessionDto();
        session.setSessionId(UUID.randomUUID().toString());
        session.setQuestionType(req.getQuestionType());
        session.setDifficulty(req.getDifficulty());
        session.setQuestions(questions.stream().map(this::toDto).collect(Collectors.toList()));
        session.setTotalQuestions(session.getQuestions().size());
        return session;
    }

    // ── Public: check answer ───────────────────────────────────────

    public CheckAnswerResponse checkAnswer(CheckAnswerRequest req) {
        Question q = questionRepository.findById(req.getQuestionId())
            .orElseThrow(() -> new AppException("Question not found", HttpStatus.NOT_FOUND));

        boolean correct = false;
        if (q.getAnswer() != null) {
            correct = q.getAnswer().trim().equalsIgnoreCase(req.getUserAnswer().trim());
        }

        CheckAnswerResponse resp = new CheckAnswerResponse();
        resp.setQuestionId(req.getQuestionId());
        resp.setCorrect(correct);
        resp.setCorrectAnswer(q.getAnswer());
        resp.setExplanation(q.getExplanation());
        return resp;
    }

    // ── Admin CRUD ─────────────────────────────────────────────────

    public List<QuestionDetailDto> getAllQuestions(String type) {
        List<Question> questions = type != null && !type.isBlank()
            ? questionRepository.findByQuestionTypeAndActiveTrue(type.toUpperCase())
            : questionRepository.findAll();
        return questions.stream().map(this::toDetailDto).collect(Collectors.toList());
    }

    @Transactional
    public QuestionDetailDto createQuestion(CreateQuestionRequest req) {
        Question q = Question.builder()
            .questionType(req.getQuestionType().toUpperCase())
            .questionText(req.getQuestionText())
            .options(req.getOptions())
            .answer(req.getAnswer())
            .explanation(req.getExplanation())
            .difficulty(capitalize(req.getDifficulty()))
            .topic(req.getTopic())
            .companyTag(req.getCompanyTag())
            .language(req.getLanguage())
            .active(req.isActive())
            .build();
        return toDetailDto(questionRepository.save(q));
    }

    @Transactional
    public QuestionDetailDto updateQuestion(Long id, CreateQuestionRequest req) {
        Question q = questionRepository.findById(id)
            .orElseThrow(() -> new AppException("Question not found", HttpStatus.NOT_FOUND));
        q.setQuestionType(req.getQuestionType().toUpperCase());
        q.setQuestionText(req.getQuestionText());
        q.setOptions(req.getOptions());
        q.setAnswer(req.getAnswer());
        q.setExplanation(req.getExplanation());
        q.setDifficulty(capitalize(req.getDifficulty()));
        q.setTopic(req.getTopic());
        q.setCompanyTag(req.getCompanyTag());
        q.setLanguage(req.getLanguage());
        q.setActive(req.isActive());
        return toDetailDto(questionRepository.save(q));
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id))
            throw new AppException("Question not found", HttpStatus.NOT_FOUND);
        questionRepository.deleteById(id);
    }

    // ── Stats ──────────────────────────────────────────────────────

    public Map<String, Long> getQuestionStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", questionRepository.count());
        stats.put("coding", questionRepository.countByQuestionTypeAndActiveTrue("CODING"));
        stats.put("aptitude", questionRepository.countByQuestionTypeAndActiveTrue("APTITUDE"));
        stats.put("systemDesign", questionRepository.countByQuestionTypeAndActiveTrue("SYSTEM_DESIGN"));
        return stats;
    }

    // ── Mappers ────────────────────────────────────────────────────

    private QuestionDto toDto(Question q) {
        QuestionDto dto = new QuestionDto();
        dto.setId(q.getId());
        dto.setQuestionType(q.getQuestionType());
        dto.setQuestionText(q.getQuestionText());
        dto.setDifficulty(q.getDifficulty());
        dto.setTopic(q.getTopic());
        dto.setCompanyTag(q.getCompanyTag());
        dto.setLanguage(q.getLanguage());
        // Parse options if present
        if (q.getOptions() != null && !q.getOptions().isBlank()) {
            dto.setOptions(Arrays.asList(q.getOptions().split("\\|\\|")));
        }
        // NEVER expose answer in public DTO
        return dto;
    }

    private QuestionDetailDto toDetailDto(Question q) {
        QuestionDetailDto dto = new QuestionDetailDto();
        dto.setId(q.getId());
        dto.setQuestionType(q.getQuestionType());
        dto.setQuestionText(q.getQuestionText());
        dto.setDifficulty(q.getDifficulty());
        dto.setTopic(q.getTopic());
        dto.setCompanyTag(q.getCompanyTag());
        dto.setLanguage(q.getLanguage());
        dto.setAnswer(q.getAnswer());
        dto.setExplanation(q.getExplanation());
        dto.setActive(q.isActive());
        dto.setCreatedAt(q.getCreatedAt());
        if (q.getOptions() != null && !q.getOptions().isBlank()) {
            dto.setOptions(Arrays.asList(q.getOptions().split("\\|\\|")));
        }
        return dto;
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
