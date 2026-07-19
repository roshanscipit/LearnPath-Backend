package com.doliuw.repository;

import com.doliuw.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuestionTypeAndActiveTrue(String questionType);

    List<Question> findByQuestionTypeAndDifficultyAndActiveTrue(String questionType, String difficulty);

    List<Question> findByCompanyTagAndActiveTrue(String companyTag);

    List<Question> findByTopicAndActiveTrue(String topic);

    /** Returns N random questions of a given type using DB-side shuffle */
    @Query(value = "SELECT * FROM questions WHERE question_type = :type AND active = true ORDER BY RAND() LIMIT :n", nativeQuery = true)
    List<Question> findRandomByType(@Param("type") String type, @Param("n") int n);

    /** Returns N random questions of a given type and difficulty */
    @Query(value = "SELECT * FROM questions WHERE question_type = :type AND difficulty = :diff AND active = true ORDER BY RAND() LIMIT :n", nativeQuery = true)
    List<Question> findRandomByTypeAndDifficulty(@Param("type") String type, @Param("diff") String difficulty, @Param("n") int n);

    /** Random questions for a specific company */
    @Query(value = "SELECT * FROM questions WHERE (company_tag = :company OR company_tag IS NULL) AND active = true ORDER BY RAND() LIMIT :n", nativeQuery = true)
    List<Question> findRandomForCompany(@Param("company") String company, @Param("n") int n);

    long countByQuestionTypeAndActiveTrue(String questionType);
}
