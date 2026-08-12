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
    @Query(value = "SELECT * FROM questions WHERE question_type = :type AND active = true ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Question> findRandomByType(@Param("type") String type, @Param("n") int n);

    /** Returns N random questions of a given type AND role (role-specific content, e.g. Java coding questions) */
    @Query(value = "SELECT * FROM questions WHERE question_type = :type AND role_tag = :role AND active = true ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Question> findRandomByTypeAndRole(@Param("type") String type, @Param("role") String role, @Param("n") int n);

    /** Returns N random questions of a given type that have NO role tag (shared/common questions, e.g. Aptitude) */
    @Query(value = "SELECT * FROM questions WHERE question_type = :type AND role_tag IS NULL AND active = true ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Question> findRandomByTypeCommon(@Param("type") String type, @Param("n") int n);

    long countByQuestionTypeAndRoleTagAndActiveTrue(String questionType, String roleTag);

    /** Returns N random questions of a given type and difficulty */
    @Query(value = "SELECT * FROM questions WHERE question_type = :type AND difficulty = :diff AND active = true ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Question> findRandomByTypeAndDifficulty(@Param("type") String type, @Param("diff") String difficulty, @Param("n") int n);

    /** Random questions for a specific company */
    @Query(value = "SELECT * FROM questions WHERE (company_tag = :company OR company_tag IS NULL) AND active = true ORDER BY RANDOM() LIMIT :n", nativeQuery = true)
    List<Question> findRandomForCompany(@Param("company") String company, @Param("n") int n);

    long countByQuestionTypeAndActiveTrue(String questionType);
}
