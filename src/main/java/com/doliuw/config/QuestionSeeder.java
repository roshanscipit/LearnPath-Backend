package com.doliuw.config;

import com.doliuw.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * One-time auto-seed for the question bank.
 *
 * On every startup this checks whether the `questions` table is empty. If it is,
 * it runs questions-seed.sql (1000+ general questions) and role-questions-seed.sql
 * (role-specific coding/system-design/behavioral questions) directly against the
 * database using Spring's quote-aware SQL script parser (safe even though some
 * question explanations contain literal semicolons, e.g. pseudocode).
 *
 * This is idempotent: once the table has rows, it's skipped on every future
 * restart/redeploy, so you never get duplicate data. No external SQL tool,
 * GUI client, or manual step is required — just deploy this file.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionSeeder implements ApplicationRunner {

    private final QuestionRepository questionRepository;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        long existing = questionRepository.count();
        if (existing > 0) {
            log.info("Question bank already has {} question(s) — skipping auto-seed.", existing);
            return;
        }

        log.info("Question bank is empty — running one-time auto-seed from questions-seed.sql and role-questions-seed.sql...");
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("questions-seed.sql"));
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("role-questions-seed.sql"));
            log.info("Auto-seed complete. Question bank now has {} question(s).", questionRepository.count());
        } catch (Exception e) {
            log.error("Auto-seed failed — question bank may be empty or partially seeded. " +
                       "You can safely redeploy to retry, or seed manually. Cause: {}", e.getMessage(), e);
        }
    }
}
