package com.doliuw.service;

import com.doliuw.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AIInterviewService
 * ─────────────────
 * Provides:
 *   • A curated question bank per IT role/variant/difficulty
 *   • Online resource links (free courses, docs, YouTube, practice sites)
 *   • In-memory session state (replace with DB/Redis for production)
 *   • Rule-based answer evaluation with keyword scoring + tips
 */
@Service
@Slf4j
public class AIInterviewService {

    // ─── In-memory session store ─────────────────────────────────────────────
    // Map<sessionId, sessionState>
    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    // Map<userId, List<sessionSummary>>
    private final Map<Long, List<Map<String, Object>>> userHistory = new ConcurrentHashMap<>();

    // ─── Public API ──────────────────────────────────────────────────────────

    /** Start a new AI mock interview session */
    public Map<String, Object> startSession(User user, String roleId, String variant, String difficulty) {
        String sessionId = UUID.randomUUID().toString();
        List<Map<String, Object>> questions = getQuestions(roleId, variant, difficulty);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("sessionId", sessionId);
        session.put("userId", user.getId());
        session.put("roleId", roleId);
        session.put("variant", variant);
        session.put("difficulty", difficulty);
        session.put("questions", questions);
        session.put("currentIndex", 0);
        session.put("answers", new ArrayList<Map<String, Object>>());
        session.put("startedAt", LocalDateTime.now().toString());
        session.put("totalQuestions", questions.size());

        sessions.put(sessionId, session);

        Map<String, Object> first = questions.get(0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        response.put("totalQuestions", questions.size());
        response.put("currentQuestionNumber", 1);
        response.put("question", first);
        response.put("roleInfo", getRoleInfo(roleId, variant));
        return response;
    }

    /** Evaluate an answer and return the next question (or finish signal) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluateAnswer(String sessionId, String questionId, String answer) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) {
            return Map.of("error", "Session not found or expired");
        }

        List<Map<String, Object>> questions = (List<Map<String, Object>>) session.get("questions");
        List<Map<String, Object>> answers   = (List<Map<String, Object>>) session.get("answers");
        int currentIndex = (int) session.get("currentIndex");

        // Find the question being answered
        Map<String, Object> currentQ = questions.stream()
            .filter(q -> q.get("id").equals(questionId))
            .findFirst()
            .orElse(questions.get(currentIndex));

        // Evaluate
        Map<String, Object> evaluation = evaluate(currentQ, answer);

        // Store answer
        Map<String, Object> answerRecord = new LinkedHashMap<>();
        answerRecord.put("questionId", questionId);
        answerRecord.put("question", currentQ.get("question"));
        answerRecord.put("userAnswer", answer);
        answerRecord.put("evaluation", evaluation);
        answers.add(answerRecord);

        // Advance index
        int nextIndex = currentIndex + 1;
        session.put("currentIndex", nextIndex);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("evaluation", evaluation);
        response.put("questionNumber", currentIndex + 1);

        if (nextIndex < questions.size()) {
            Map<String, Object> nextQ = questions.get(nextIndex);
            response.put("hasNextQuestion", true);
            response.put("nextQuestion", nextQ);
            response.put("nextQuestionNumber", nextIndex + 1);
        } else {
            response.put("hasNextQuestion", false);
            response.put("message", "All questions answered! Call /finish to get your report.");
        }
        return response;
    }

    /** Finish the session and return a full scorecard */
    @SuppressWarnings("unchecked")
    public Map<String, Object> finishSession(String sessionId, User user) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) {
            return Map.of("error", "Session not found or expired");
        }

        List<Map<String, Object>> answers = (List<Map<String, Object>>) session.get("answers");
        int total = answers.size();

        int totalScore = answers.stream()
            .mapToInt(a -> {
                Map<String, Object> ev = (Map<String, Object>) a.get("evaluation");
                return (int) ev.getOrDefault("score", 0);
            }).sum();

        int maxScore = total * 10;
        int percent  = maxScore > 0 ? (totalScore * 100) / maxScore : 0;

        String grade = percent >= 80 ? "Excellent" : percent >= 60 ? "Good" : percent >= 40 ? "Average" : "Needs Improvement";

        // Build scorecard
        Map<String, Object> scorecard = new LinkedHashMap<>();
        scorecard.put("sessionId", sessionId);
        scorecard.put("roleId", session.get("roleId"));
        scorecard.put("variant", session.get("variant"));
        scorecard.put("difficulty", session.get("difficulty"));
        scorecard.put("totalQuestions", session.get("totalQuestions"));
        scorecard.put("answeredQuestions", total);
        scorecard.put("totalScore", totalScore);
        scorecard.put("maxScore", maxScore);
        scorecard.put("percentage", percent);
        scorecard.put("grade", grade);
        scorecard.put("completedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        scorecard.put("answers", answers);
        scorecard.put("resources", getResources((String) session.get("roleId")));
        scorecard.put("nextSteps", getNextSteps(percent, (String) session.get("roleId")));

        // Save to user history
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("roleId", session.get("roleId"));
        summary.put("variant", session.get("variant"));
        summary.put("difficulty", session.get("difficulty"));
        summary.put("score", percent);
        summary.put("grade", grade);
        summary.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        userHistory.computeIfAbsent(user.getId(), k -> new ArrayList<>()).add(0, summary);

        // Clean up session
        sessions.remove(sessionId);
        return scorecard;
    }

    /** Get user's past sessions */
    public List<Map<String, Object>> getUserHistory(User user) {
        return userHistory.getOrDefault(user.getId(), List.of());
    }

    /** Return all IT roles with online resources for interviews */
    public List<Map<String, Object>> getInterviewRoles() {
        List<Map<String, Object>> roles = new ArrayList<>();

        roles.add(interviewRole("java", "Java Developer", "Code",
            List.of("Full Stack", "Backend", "Frontend"),
            getResources("java")));

        roles.add(interviewRole("python", "Python Developer", "Code",
            List.of("Django/Flask", "Data Science", "AI/ML", "Automation"),
            getResources("python")));

        roles.add(interviewRole("dotnet", ".NET Developer", "Code2",
            List.of("Full Stack", "Backend", "Frontend"),
            getResources("dotnet")));

        roles.add(interviewRole("devops", "DevOps Engineer", "Settings",
            List.of("AWS", "Azure", "GCP", "Multi-Cloud"),
            getResources("devops")));

        roles.add(interviewRole("data-engineer", "Data Engineer", "BarChart",
            List.of("ETL/ELT", "Big Data", "Analytics", "ML Ops"),
            getResources("data-engineer")));

        roles.add(interviewRole("testing", "QA/Testing", "TestTube2",
            List.of("Manual", "Automation", "Performance"),
            getResources("testing")));

        roles.add(interviewRole("mobile", "Mobile Developer", "Smartphone",
            List.of("Android (Kotlin)", "iOS (Swift)", "React Native", "Flutter"),
            getResources("mobile")));

        roles.add(interviewRole("cybersecurity", "Cybersecurity Analyst", "Shield",
            List.of("SOC Analyst", "Penetration Testing", "Cloud Security", "AppSec"),
            getResources("cybersecurity")));

        roles.add(interviewRole("salesforce", "Salesforce Developer", "Cloud",
            List.of("Developer", "Admin", "Architect"),
            getResources("salesforce")));

        roles.add(interviewRole("sap", "SAP Consultant", "Database",
            List.of("MM", "PTP", "BTP", "Sales", "Integration"),
            getResources("sap")));

        return roles;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /** Rule-based evaluator: score 0-10, feedback, model answer hint, tip */
    private Map<String, Object> evaluate(Map<String, Object> question, String answer) {
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) question.getOrDefault("keywords", List.of());
        String modelAnswer    = (String) question.getOrDefault("modelAnswer", "");
        String tip            = (String) question.getOrDefault("tip", "");

        String lAnswer = answer == null ? "" : answer.toLowerCase();
        long matched = keywords.stream().filter(k -> lAnswer.contains(k.toLowerCase())).count();
        int keywordScore = keywords.isEmpty() ? 5 : (int) Math.min(5, (matched * 5) / keywords.size());

        // Length / depth bonus
        int lengthScore = Math.min(5, answer != null ? answer.trim().split("\\s+").length / 20 : 0);
        int total = Math.min(10, keywordScore + lengthScore);

        String feedback;
        if (total >= 8)       feedback = "Excellent answer! You covered the key concepts well.";
        else if (total >= 6)  feedback = "Good answer. You captured most of the important points.";
        else if (total >= 4)  feedback = "Decent attempt. Try to include more technical specifics.";
        else if (total >= 2)  feedback = "Partial answer. Review the concept and try again.";
        else                  feedback = "The answer needs more depth. Study this topic carefully.";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", total);
        result.put("maxScore", 10);
        result.put("feedback", feedback);
        result.put("modelAnswer", modelAnswer);
        result.put("tip", tip);
        result.put("keywordsMatched", matched);
        result.put("totalKeywords", keywords.size());
        return result;
    }

    /** Short role info for the session header */
    private Map<String, Object> getRoleInfo(String roleId, String variant) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("roleId", roleId);
        info.put("variant", variant);
        info.put("resources", getResources(roleId));
        return info;
    }

    private Map<String, Object> interviewRole(String id, String name, String icon,
                                               List<String> variants,
                                               List<Map<String, Object>> resources) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", name); m.put("icon", icon);
        m.put("variants", variants); m.put("resources", resources);
        return m;
    }

    // ─── Online Resources per role ────────────────────────────────────────────

    /** Curated free online resources for each IT role */
    public List<Map<String, Object>> getResources(String roleId) {
        return switch (roleId) {
            case "java" -> List.of(
                resource("Java Documentation", "https://docs.oracle.com/en/java/", "docs", "Official Oracle Java Docs"),
                resource("Spring Boot Docs", "https://spring.io/projects/spring-boot", "docs", "Official Spring Boot reference"),
                resource("Java by Baeldung", "https://www.baeldung.com/", "article", "In-depth Java & Spring tutorials"),
                resource("LeetCode Java", "https://leetcode.com/problemset/?difficulty=EASY&page=1&topicSlugs=java", "practice", "Java DSA practice problems"),
                resource("Java Full Course – FreeCodeCamp", "https://www.youtube.com/watch?v=GoXwIVyNvX0", "video", "12-hour Java beginner to advanced"),
                resource("HackerRank Java", "https://www.hackerrank.com/domains/java", "practice", "Java skill challenges"),
                resource("Java Design Patterns", "https://refactoring.guru/design-patterns/java", "article", "GOF patterns in Java")
            );
            case "python" -> List.of(
                resource("Python Docs", "https://docs.python.org/3/", "docs", "Official Python 3 reference"),
                resource("Real Python", "https://realpython.com/", "article", "In-depth Python tutorials"),
                resource("Django Docs", "https://docs.djangoproject.com/", "docs", "Official Django framework docs"),
                resource("FastAPI Docs", "https://fastapi.tiangolo.com/", "docs", "Modern Python API framework"),
                resource("Python for ML – Kaggle", "https://www.kaggle.com/learn/python", "course", "Free interactive Kaggle course"),
                resource("Python Full Course – FreeCodeCamp", "https://www.youtube.com/watch?v=rfscVS0vtbw", "video", "4-hour Python beginner course"),
                resource("LeetCode Python", "https://leetcode.com/problemset/?difficulty=EASY&page=1", "practice", "DSA practice in Python")
            );
            case "dotnet" -> List.of(
                resource(".NET Documentation", "https://learn.microsoft.com/en-us/dotnet/", "docs", "Official Microsoft .NET docs"),
                resource("ASP.NET Core Docs", "https://learn.microsoft.com/en-us/aspnet/core/", "docs", "Web API & MVC reference"),
                resource("C# Programming Guide", "https://learn.microsoft.com/en-us/dotnet/csharp/", "docs", "Language reference"),
                resource("Microsoft Learn .NET", "https://learn.microsoft.com/en-us/training/dotnet/", "course", "Free interactive .NET learning paths"),
                resource(".NET Tutorial – FreeCodeCamp", "https://www.youtube.com/watch?v=GhQdlIFylQ8", "video", "C# full tutorial"),
                resource("NuGet Gallery", "https://www.nuget.org/", "docs", ".NET package repository")
            );
            case "devops" -> List.of(
                resource("AWS Free Training", "https://explore.skillbuilder.aws/learn", "course", "AWS Skill Builder – free courses"),
                resource("Azure Learning Path", "https://learn.microsoft.com/en-us/training/azure/", "course", "Microsoft Learn Azure paths"),
                resource("Kubernetes Docs", "https://kubernetes.io/docs/home/", "docs", "Official K8s documentation"),
                resource("Docker Docs", "https://docs.docker.com/", "docs", "Official Docker docs"),
                resource("Terraform by HashiCorp", "https://developer.hashicorp.com/terraform/tutorials", "course", "Free Terraform tutorials"),
                resource("DevOps Roadmap", "https://roadmap.sh/devops", "article", "Community-curated DevOps roadmap"),
                resource("Linux Command Line – FreeCodeCamp", "https://www.youtube.com/watch?v=ZtqBQ68cfJc", "video", "Linux basics for DevOps")
            );
            case "data-engineer" -> List.of(
                resource("Apache Spark Docs", "https://spark.apache.org/docs/latest/", "docs", "Official PySpark & Spark docs"),
                resource("dbt Docs", "https://docs.getdbt.com/", "docs", "dbt analytics engineering reference"),
                resource("Airflow Docs", "https://airflow.apache.org/docs/", "docs", "Apache Airflow orchestration docs"),
                resource("Snowflake Quickstarts", "https://quickstarts.snowflake.com/", "course", "Free Snowflake tutorials"),
                resource("Data Engineering Zoomcamp", "https://github.com/DataTalksClub/data-engineering-zoomcamp", "course", "Free 9-week DE bootcamp"),
                resource("Kaggle SQL", "https://www.kaggle.com/learn/advanced-sql", "course", "Free advanced SQL course"),
                resource("DE Roadmap", "https://roadmap.sh/data-engineer", "article", "Data Engineering learning roadmap")
            );
            case "testing" -> List.of(
                resource("Selenium Docs", "https://www.selenium.dev/documentation/", "docs", "Official Selenium WebDriver docs"),
                resource("Cypress Docs", "https://docs.cypress.io/", "docs", "Cypress E2E testing reference"),
                resource("Playwright Docs", "https://playwright.dev/docs/intro", "docs", "MS Playwright automation docs"),
                resource("ISTQB Foundation", "https://www.istqb.org/certifications/certified-tester-foundation-level", "docs", "ISTQB certification syllabus"),
                resource("JMeter Tutorial", "https://jmeter.apache.org/usermanual/index.html", "docs", "Apache JMeter performance testing"),
                resource("QA Automation – FreeCodeCamp", "https://www.youtube.com/watch?v=nu1E0sE3QFo", "video", "Selenium Java automation course"),
                resource("HackerRank SQL", "https://www.hackerrank.com/domains/sql", "practice", "SQL skills for test data querying")
            );
            case "mobile" -> List.of(
                resource("Android Developers", "https://developer.android.com/courses", "course", "Official Android dev training"),
                resource("Swift by Apple", "https://developer.apple.com/swift/", "docs", "Official Swift language docs"),
                resource("React Native Docs", "https://reactnative.dev/docs/getting-started", "docs", "Official RN docs"),
                resource("Flutter Docs", "https://docs.flutter.dev/", "docs", "Official Flutter documentation"),
                resource("Jetpack Compose Pathway", "https://developer.android.com/courses/pathways/jetpack-compose", "course", "Free Google Compose codelab"),
                resource("100 Days of SwiftUI", "https://www.hackingwithswift.com/100/swiftui", "course", "Free SwiftUI challenge"),
                resource("Flutter Crash Course", "https://www.youtube.com/watch?v=1ukSR1GRtMU", "video", "Flutter beginner tutorial")
            );
            case "cybersecurity" -> List.of(
                resource("TryHackMe", "https://tryhackme.com/", "practice", "Gamified cybersecurity learning labs"),
                resource("Hack The Box", "https://www.hackthebox.com/", "practice", "Advanced penetration testing challenges"),
                resource("OWASP Top 10", "https://owasp.org/www-project-top-ten/", "docs", "Top web security vulnerabilities"),
                resource("CompTIA Security+ Study", "https://www.professormesser.com/security-plus/sy0-701/sy0-701-video/sy0-701-comptia-security-plus-course/", "course", "Free Security+ video course"),
                resource("NIST Cybersecurity Framework", "https://www.nist.gov/cyberframework", "docs", "NIST security standards & best practices"),
                resource("Cybersecurity Roadmap", "https://roadmap.sh/cyber-security", "article", "Community roadmap for security"),
                resource("TCM Security – YouTube", "https://www.youtube.com/c/TCMSecurityAcademy", "video", "Free ethical hacking tutorials")
            );
            case "salesforce" -> List.of(
                resource("Trailhead", "https://trailhead.salesforce.com/", "course", "Official Salesforce learning platform – free"),
                resource("Apex Developer Guide", "https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/", "docs", "Official Apex language reference"),
                resource("LWC Docs", "https://developer.salesforce.com/docs/component-library/documentation/en/lwc", "docs", "Lightning Web Components docs"),
                resource("SOQL/SOSL Reference", "https://developer.salesforce.com/docs/atlas.en-us.soql_sosl.meta/soql_sosl/", "docs", "Query language reference"),
                resource("Salesforce YouTube", "https://www.youtube.com/c/salesforce", "video", "Official Salesforce video tutorials"),
                resource("Focus on Force", "https://focusonforce.com/", "course", "Salesforce certification study guides")
            );
            case "sap" -> List.of(
                resource("SAP Learning Hub", "https://learning.sap.com/", "course", "Official SAP learning – free tier available"),
                resource("SAP Help Portal", "https://help.sap.com/", "docs", "Official SAP product documentation"),
                resource("SAP Community", "https://community.sap.com/", "article", "SAP developer community Q&A"),
                resource("SAP BTP Tutorials", "https://developers.sap.com/tutorial-navigator.html", "course", "SAP BTP hands-on tutorials"),
                resource("SAP YouTube", "https://www.youtube.com/user/SAPtechEd", "video", "SAP TechEd sessions"),
                resource("ERProof SAP MM", "https://erproof.com/mm/", "article", "SAP MM module training articles")
            );
            default -> List.of(
                resource("MDN Web Docs", "https://developer.mozilla.org/", "docs", "Web technology reference"),
                resource("FreeCodeCamp", "https://www.freecodecamp.org/", "course", "Free full-stack curriculum"),
                resource("LeetCode", "https://leetcode.com/", "practice", "Coding interview problems")
            );
        };
    }

    private Map<String, Object> resource(String name, String url, String type, String description) {
        return Map.of("name", name, "url", url, "type", type, "description", description);
    }

    private List<String> getNextSteps(int percent, String roleId) {
        List<Map<String, Object>> res = getResources(roleId);
        List<String> steps = new ArrayList<>();
        if (percent < 40) {
            steps.add("Start with the official documentation and free courses listed below.");
            steps.add("Practice basic concepts daily for at least 1 hour.");
            steps.add("Try beginner-level problems on LeetCode / HackerRank.");
        } else if (percent < 70) {
            steps.add("Strengthen your weak areas identified in this interview.");
            steps.add("Work on 2-3 medium-level coding problems daily.");
            steps.add("Build a project to apply your knowledge practically.");
        } else {
            steps.add("Great job! Focus on advanced topics and system design.");
            steps.add("Contribute to open-source projects for experience.");
            steps.add("Attempt hard-level interview questions and mock company-specific tests.");
        }
        return steps;
    }

    // ─── Question Bank ────────────────────────────────────────────────────────

    private List<Map<String, Object>> getQuestions(String roleId, String variant, String difficulty) {
        List<Map<String, Object>> all = allQuestions().getOrDefault(roleId, List.of());
        List<Map<String, Object>> filtered = all.stream()
            .filter(q -> {
                String qDiff = (String) q.getOrDefault("difficulty", "Medium");
                String qVariant = (String) q.getOrDefault("variant", "");
                boolean diffMatch = difficulty.equals("All") || qDiff.equalsIgnoreCase(difficulty);
                boolean variantMatch = qVariant.isEmpty() || qVariant.equals(variant);
                return diffMatch && variantMatch;
            })
            .toList();

        // If no variant-specific questions, fall back to general ones
        if (filtered.isEmpty()) filtered = all;

        // Return up to 8 questions shuffled
        List<Map<String, Object>> result = new ArrayList<>(filtered);
        Collections.shuffle(result);
        return result.subList(0, Math.min(8, result.size()));
    }

    private Map<String, List<Map<String, Object>>> allQuestions() {
        Map<String, List<Map<String, Object>>> bank = new HashMap<>();

        // ── Java Questions ───────────────────────────────────────────────────
        bank.put("java", List.of(
            question("j1", "What is the difference between JDK, JRE, and JVM?", "Easy", "",
                List.of("JDK", "JRE", "JVM", "compiler", "runtime", "bytecode"),
                "JDK (Java Development Kit) includes the compiler and tools to develop Java programs. JRE (Java Runtime Environment) provides the runtime to execute Java programs. JVM (Java Virtual Machine) is the engine that interprets bytecode and enables platform independence.",
                "Remember: JDK = JRE + Dev tools. JRE = JVM + Libraries. JVM executes bytecode."),

            question("j2", "Explain OOP principles in Java with examples.", "Medium", "",
                List.of("encapsulation", "inheritance", "polymorphism", "abstraction", "class", "interface"),
                "OOP stands for Object-Oriented Programming. Encapsulation hides internal state. Inheritance allows a class to extend another. Polymorphism enables one interface to be used for different types. Abstraction hides complex implementation details.",
                "Use real-world analogies: a Car class extending Vehicle, or a Shape interface with Circle/Square implementations."),

            question("j3", "What are Java Generics and why are they used?", "Medium", "Backend",
                List.of("generics", "type-safe", "erasure", "wildcard", "bounded", "List<T>"),
                "Generics provide type safety at compile time, eliminate the need for casting, and enable reusable code. Type erasure removes generic type info at runtime. Wildcards (?) allow flexible type parameters.",
                "Common usage: Collections like List<String>, Map<K,V>, and custom generic utility classes."),

            question("j4", "Explain the difference between HashMap and ConcurrentHashMap.", "Medium", "Backend",
                List.of("HashMap", "ConcurrentHashMap", "thread-safe", "synchronized", "segment", "null key"),
                "HashMap is not thread-safe. ConcurrentHashMap is thread-safe using segment-level locking (Java 7) or CAS operations (Java 8+). HashMap allows one null key; ConcurrentHashMap does not. ConcurrentHashMap has better concurrent performance than a synchronized HashMap.",
                "Use ConcurrentHashMap in multi-threaded environments to avoid ConcurrentModificationException."),

            question("j5", "What is Spring Boot and why is it preferred over traditional Spring?", "Medium", "Full Stack",
                List.of("Spring Boot", "auto-configuration", "embedded server", "starters", "opinionated", "Spring"),
                "Spring Boot provides auto-configuration, embedded Tomcat/Jetty, and starter dependencies that eliminate boilerplate. It follows convention over configuration. Traditional Spring requires manual XML/Java config for every component.",
                "Spring Boot's @SpringBootApplication annotation combines @Configuration, @EnableAutoConfiguration, and @ComponentScan."),

            question("j6", "What is the difference between @RestController and @Controller in Spring?", "Easy", "Full Stack",
                List.of("@RestController", "@Controller", "@ResponseBody", "JSON", "view", "REST"),
                "@RestController = @Controller + @ResponseBody on every method. It returns data (typically JSON/XML) directly. @Controller is used in MVC apps where methods return view names for template rendering.",
                "Use @RestController for REST APIs, @Controller for traditional server-side rendered views."),

            question("j7", "What are Java Streams and how do they improve code readability?", "Medium", "Backend",
                List.of("Stream", "lambda", "filter", "map", "reduce", "collect", "functional"),
                "Java Streams (Java 8+) enable functional-style operations on collections: filter, map, reduce, collect. They improve readability by replacing verbose loops with declarative pipelines. Streams are lazy — operations execute only when a terminal operation (collect, count, findFirst) is invoked.",
                "Example: list.stream().filter(x -> x > 5).map(String::valueOf).collect(Collectors.toList())"),

            question("j8", "How does JPA/Hibernate work and what is the N+1 problem?", "Hard", "Backend",
                List.of("JPA", "Hibernate", "ORM", "N+1", "lazy loading", "fetch", "JPQL", "entity"),
                "JPA is the specification; Hibernate is the implementation. ORM maps Java entities to DB tables. The N+1 problem occurs when fetching a list of N entities causes N additional queries for each association. Fix it with JOIN FETCH, @BatchSize, or @EntityGraph.",
                "Always profile queries in dev using spring.jpa.show-sql=true and look for repeated queries.")
        ));

        // ── Python Questions ─────────────────────────────────────────────────
        bank.put("python", List.of(
            question("py1", "What are Python decorators and how do they work?", "Medium", "",
                List.of("decorator", "@", "wrapper", "function", "closure", "functools"),
                "A decorator is a function that wraps another function to extend its behaviour without modifying it. They are applied with the @ syntax. Under the hood they use closures. functools.wraps preserves the wrapped function's metadata.",
                "Common built-in decorators: @staticmethod, @classmethod, @property, @functools.lru_cache"),

            question("py2", "Explain the difference between a list, tuple, and set in Python.", "Easy", "",
                List.of("list", "tuple", "set", "mutable", "immutable", "ordered", "duplicate"),
                "List: ordered, mutable, allows duplicates. Tuple: ordered, immutable, allows duplicates. Set: unordered, mutable, no duplicates. Use tuples for fixed data, sets for membership testing.",
                "List = [], Tuple = (), Set = {}. Frozen sets are immutable sets."),

            question("py3", "What is the GIL in Python and how does it affect multithreading?", "Hard", "",
                List.of("GIL", "Global Interpreter Lock", "thread", "multiprocessing", "CPython", "IO-bound", "CPU-bound"),
                "The GIL is a mutex in CPython that allows only one thread to execute Python bytecode at a time. IO-bound tasks benefit from threads (waiting releases the GIL). CPU-bound tasks should use multiprocessing to bypass the GIL.",
                "Alternatives: multiprocessing module, asyncio for async IO, or Cython/C extensions."),

            question("py4", "How does Django's ORM work? Explain QuerySets.", "Medium", "Django/Flask",
                List.of("ORM", "QuerySet", "lazy", "filter", "select_related", "prefetch_related", "Django"),
                "Django ORM maps Python classes (models) to DB tables. QuerySets are lazy – they build SQL but only hit the DB when evaluated (iteration, slicing, etc.). select_related does SQL JOIN for FK relations; prefetch_related does separate queries for M2M.",
                "Use .explain() to debug query plans and avoid N+1 issues with prefetch_related."),

            question("py5", "Explain async/await in Python and when to use it.", "Hard", "Django/Flask",
                List.of("async", "await", "asyncio", "coroutine", "event loop", "aiohttp", "IO-bound"),
                "async/await enables non-blocking IO in a single thread via an event loop. Use it for IO-bound tasks (HTTP calls, DB queries) where threads are wasteful. asyncio.gather() runs multiple coroutines concurrently.",
                "FastAPI and ASGI Django support native async views. Don't use async for CPU-bound work — use multiprocessing instead."),

            question("py6", "What are Python generators and how do they save memory?", "Medium", "",
                List.of("generator", "yield", "lazy", "iterator", "memory", "next()"),
                "Generators produce values lazily using yield instead of building an entire list. They implement the iterator protocol. A generator function returns a generator object. Use them for large data streams to save memory.",
                "Example: def squares(n): for i in range(n): yield i**2")
        ));

        // ── DevOps Questions ─────────────────────────────────────────────────
        bank.put("devops", List.of(
            question("dv1", "What is the difference between Docker containers and virtual machines?", "Easy", "",
                List.of("container", "VM", "hypervisor", "OS kernel", "lightweight", "isolated", "Docker"),
                "VMs virtualise the entire hardware stack including the OS kernel via a hypervisor. Containers share the host OS kernel and virtualise only at the process level, making them lightweight and fast to start. Containers are not full isolation like VMs.",
                "Docker uses namespaces and cgroups for isolation. VMs use VMware, Hyper-V, or KVM."),

            question("dv2", "Explain CI/CD and the difference between Continuous Delivery and Continuous Deployment.", "Medium", "",
                List.of("CI", "CD", "continuous integration", "continuous delivery", "pipeline", "deployment", "automated"),
                "CI: automatically build and test code on every commit. Continuous Delivery: code is always in a deployable state but deployment is manual. Continuous Deployment: every passing pipeline automatically deploys to production.",
                "Tools: Jenkins, GitHub Actions, GitLab CI, CircleCI, AWS CodePipeline."),

            question("dv3", "What is Kubernetes and what problems does it solve?", "Medium", "",
                List.of("Kubernetes", "orchestration", "pod", "deployment", "service", "scaling", "self-healing"),
                "Kubernetes automates deployment, scaling, and management of containerised apps. It solves container orchestration: scheduling pods across nodes, self-healing (restarting failed pods), horizontal scaling, load balancing, and rolling updates.",
                "Core objects: Pod, Deployment, Service, ConfigMap, Secret, Ingress."),

            question("dv4", "What is Infrastructure as Code (IaC) and why is Terraform popular?", "Medium", "",
                List.of("IaC", "Terraform", "declarative", "state", "provider", "plan", "apply"),
                "IaC manages infrastructure through code instead of manual processes, enabling version control and repeatability. Terraform uses a declarative HCL syntax, maintains a state file to track resources, and supports 1000+ cloud providers.",
                "Terraform workflow: terraform init → terraform plan → terraform apply → terraform destroy"),

            question("dv5", "How do you handle secrets in a Kubernetes environment?", "Hard", "",
                List.of("Secret", "Kubernetes", "base64", "Vault", "KMS", "RBAC", "environment variable"),
                "Kubernetes Secrets store sensitive data as base64-encoded values. They should not be stored in Git. Best practices: use HashiCorp Vault, AWS Secrets Manager, or Azure Key Vault with the secrets-store-csi-driver. Restrict access via RBAC.",
                "Never commit .env files or k8s secret manifests with actual values to source control.")
        ));

        // ── Data Engineer Questions ───────────────────────────────────────────
        bank.put("data-engineer", List.of(
            question("de1", "What is the difference between ETL and ELT?", "Easy", "",
                List.of("ETL", "ELT", "extract", "transform", "load", "data warehouse", "cloud"),
                "ETL: Extract → Transform (in an intermediate server) → Load into warehouse. ELT: Extract → Load into warehouse → Transform inside the warehouse. ELT is preferred in cloud-native setups (Snowflake, BigQuery) where transformation compute is cheap.",
                "dbt is used for the T in ELT – transforming data already loaded into the warehouse."),

            question("de2", "Explain the concept of data partitioning in Spark.", "Hard", "Big Data",
                List.of("partition", "Spark", "shuffle", "repartition", "coalesce", "skew", "parallelism"),
                "Partitioning divides data into chunks processed in parallel across cluster nodes. repartition() reshuffles all data to N partitions (expensive). coalesce() reduces partitions without full shuffle (cheaper). Data skew causes some partitions to be much larger, creating bottlenecks.",
                "Ideal partition size is 128MB-256MB. Use salting to handle skewed keys."),

            question("de3", "What is Apache Kafka and how does it differ from a traditional message queue?", "Medium", "",
                List.of("Kafka", "topic", "partition", "consumer group", "offset", "retention", "log"),
                "Kafka is a distributed event streaming platform. Unlike traditional queues, messages persist for a configurable retention period and can be replayed. Multiple consumer groups can independently read the same topic. Kafka uses partitions for parallelism and replication for fault tolerance.",
                "Kafka is pull-based (consumers pull), traditional queues are push-based."),

            question("de4", "Explain slowly changing dimensions (SCD) types in data warehousing.", "Hard", "Analytics",
                List.of("SCD", "Type 1", "Type 2", "Type 3", "dimension", "history", "surrogate key"),
                "SCD manages changes in dimension data. Type 1: overwrite the old value (no history). Type 2: add a new row with valid_from/valid_to dates and a surrogate key (full history). Type 3: add a previous_value column (limited history).",
                "Type 2 is most common in enterprise data warehouses for tracking customer or product changes over time.")
        ));

        // ── Testing Questions ────────────────────────────────────────────────
        bank.put("testing", List.of(
            question("t1", "What is the difference between unit testing, integration testing, and E2E testing?", "Easy", "",
                List.of("unit", "integration", "E2E", "mock", "stub", "selenium", "isolated"),
                "Unit tests verify a single function/class in isolation, often using mocks. Integration tests verify multiple components working together. E2E tests simulate real user flows from browser to database.",
                "Testing pyramid: many unit tests, fewer integration, fewer E2E. Cost and speed are the tradeoff."),

            question("t2", "What is the Page Object Model (POM) in Selenium automation?", "Medium", "Automation",
                List.of("Page Object Model", "POM", "Selenium", "locator", "reusability", "maintenance", "class"),
                "POM is a design pattern where each web page has a dedicated class containing its locators and actions. This improves reusability, readability, and maintainability. Test scripts only interact with page objects, not raw WebDriver calls.",
                "Example: A LoginPage class with methods like enterUsername(), enterPassword(), clickLogin()"),

            question("t3", "How do you approach performance testing and what metrics do you track?", "Hard", "Performance",
                List.of("JMeter", "response time", "throughput", "error rate", "load", "stress", "spike", "concurrent"),
                "Performance testing types: load (normal usage), stress (beyond capacity), spike (sudden surge), soak (extended duration). Key metrics: response time (p95, p99), throughput (req/sec), error rate, CPU/memory utilization.",
                "JMeter and Gatling are popular tools. Always test in a production-like environment.")
        ));

        // ── Cybersecurity Questions ──────────────────────────────────────────
        bank.put("cybersecurity", List.of(
            question("cs1", "What is SQL Injection and how do you prevent it?", "Easy", "",
                List.of("SQL injection", "prepared statement", "parameterized", "input validation", "ORM", "sanitize"),
                "SQL injection occurs when unsanitised user input is embedded in SQL queries, allowing attackers to manipulate the database. Prevention: use prepared statements/parameterized queries, ORMs, input validation, and least-privilege DB accounts.",
                "OWASP lists SQL injection as a top-10 web vulnerability. Always use parameterized queries."),

            question("cs2", "Explain the CIA triad in cybersecurity.", "Easy", "",
                List.of("confidentiality", "integrity", "availability", "CIA", "encryption", "hash", "backup"),
                "CIA Triad: Confidentiality (data accessible only to authorised parties – encryption), Integrity (data is accurate and untampered – hashing, signatures), Availability (systems accessible when needed – redundancy, backups).",
                "Almost every security control can be mapped to one or more CIA properties."),

            question("cs3", "What is the OWASP Top 10 and why is it important?", "Medium", "AppSec",
                List.of("OWASP", "injection", "broken auth", "XSS", "IDOR", "security misconfiguration", "top 10"),
                "OWASP Top 10 is a standard awareness document for web application security risks, updated every 3-4 years. Current top risks include Broken Access Control, Cryptographic Failures, Injection, Insecure Design, and Security Misconfiguration.",
                "Use it as a baseline checklist when performing security reviews or penetration testing.")
        ));

        // ── Mobile Questions ─────────────────────────────────────────────────
        bank.put("mobile", List.of(
            question("m1", "What is the difference between React Native and Flutter?", "Easy", "",
                List.of("React Native", "Flutter", "Dart", "JavaScript", "native", "bridge", "widget"),
                "React Native uses JavaScript and renders native components via a bridge. Flutter uses Dart and renders its own widgets via Skia/Impeller engine – no bridge. Flutter offers better performance and consistency across platforms; React Native has a larger ecosystem.",
                "React Native is closer to native look-and-feel; Flutter gives pixel-perfect custom UI."),

            question("m2", "Explain Android Activity Lifecycle.", "Medium", "Android (Kotlin)",
                List.of("onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy", "lifecycle"),
                "Android Activity lifecycle: onCreate (initialise) → onStart (visible) → onResume (interactive) → onPause (losing focus) → onStop (not visible) → onDestroy (cleaned up). onRestart is called after onStop if the activity comes back.",
                "Always release resources in onPause/onStop and restore state in onCreate with savedInstanceState."),

            question("m3", "What is Jetpack Compose and how does it differ from XML-based layouts?", "Medium", "Android (Kotlin)",
                List.of("Jetpack Compose", "composable", "declarative", "XML", "recomposition", "state", "UI"),
                "Jetpack Compose is Android's modern declarative UI toolkit. Instead of XML layouts you write @Composable functions in Kotlin. The UI recomposes when state changes. It eliminates the need for View binding, adapters, and most boilerplate.",
                "Key concepts: @Composable, remember {}, State hoisting, LaunchedEffect, ViewModel integration.")
        ));

        // ── Salesforce Questions ─────────────────────────────────────────────
        bank.put("salesforce", List.of(
            question("sf1", "What is Apex and how is it similar to Java?", "Easy", "Developer",
                List.of("Apex", "Java", "strongly typed", "trigger", "governor limits", "SOQL", "object-oriented"),
                "Apex is Salesforce's proprietary Java-like language for backend logic. It is strongly typed, OOP, runs on Salesforce servers, and is subject to governor limits (CPU time, SOQL queries, heap size). Key constructs: classes, triggers, test classes.",
                "Unlike Java, Apex has native SOQL/SOSL support and governor limits enforced per transaction."),

            question("sf2", "What are Governor Limits in Salesforce and why do they exist?", "Medium", "Developer",
                List.of("governor limits", "SOQL", "DML", "CPU", "heap", "multi-tenant", "transaction"),
                "Governor limits enforce resource boundaries per transaction in Salesforce's multi-tenant cloud. Limits include 100 SOQL queries, 150 DML operations, 10MB heap, 10 seconds CPU per transaction. They ensure fair resource sharing across all tenants.",
                "Bulkify your code: always process records in collections, never put SOQL/DML inside loops."),

            question("sf3", "What is the difference between a Flow and Apex Trigger in Salesforce?", "Medium", "",
                List.of("Flow", "trigger", "declarative", "programmatic", "automation", "process builder"),
                "Flow is a declarative, no-code/low-code automation tool built in the UI. Apex Triggers are programmatic, written in Apex, and offer full flexibility. Salesforce recommends Flows first; use Triggers for complex logic Flows can't handle.",
                "Flow types: Screen Flow, Record-Triggered Flow, Schedule-Triggered Flow, Autolaunched Flow.")
        ));

        // ── SAP Questions ────────────────────────────────────────────────────
        bank.put("sap", List.of(
            question("sap1", "What is SAP and what does it stand for?", "Easy", "",
                List.of("SAP", "ERP", "enterprise", "modules", "integration", "business processes"),
                "SAP stands for Systems, Applications, and Products in Data Processing. It is the world's leading ERP (Enterprise Resource Planning) system. SAP integrates business processes across finance, HR, supply chain, manufacturing, and more in a unified platform.",
                "SAP S/4HANA is the latest version; SAP ECC is the older on-premise version."),

            question("sap2", "Explain the Purchase-to-Pay (P2P) process in SAP MM.", "Medium", "PTP",
                List.of("purchase requisition", "purchase order", "goods receipt", "invoice", "payment", "MM", "FI"),
                "P2P cycle: 1) Purchase Requisition (PR) created → 2) PO created → 3) Goods Receipt (GR) posted → 4) Invoice Verification (MIRO) → 5) Payment via FI. The GR/IR clearing account reconciles goods received vs invoiced.",
                "Key t-codes: ME51N (PR), ME21N (PO), MIGO (GR), MIRO (Invoice Verification)"),

            question("sap3", "What is SAP BTP and what are its main services?", "Medium", "BTP",
                List.of("BTP", "Business Technology Platform", "Cloud Foundry", "HANA Cloud", "Integration Suite", "CAP", "extension"),
                "SAP BTP is SAP's cloud platform for building, integrating, and extending SAP applications. Key services: HANA Cloud (database), Integration Suite (iPaaS), CAP Framework (app development), Analytics Cloud, and Extension Suite for extending S/4HANA.",
                "BTP follows a multi-cloud strategy supporting AWS, Azure, and GCP hyperscalers.")
        ));

        return bank;
    }

    private Map<String, Object> question(String id, String question, String difficulty,
                                          String variant, List<String> keywords,
                                          String modelAnswer, String tip) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("id", id);
        q.put("question", question);
        q.put("difficulty", difficulty);
        q.put("variant", variant);
        q.put("keywords", keywords);
        q.put("modelAnswer", modelAnswer);
        q.put("tip", tip);
        return q;
    }
}
