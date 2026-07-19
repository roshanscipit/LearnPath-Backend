package com.doliuw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final ObjectMapper objectMapper;

    // ─── Static data (same as frontend mockData) ──────────────────
    // Cached in Caffeine for 30 minutes – avoids rebuilding on every request

    @Cacheable(value = "companies")
    public List<Map<String, Object>> getAllCompanies() {
        log.debug("Loading companies into cache");
        return buildCompanies();
    }

    @Cacheable(value = "companies", key = "'company_' + #id")
    public Map<String, Object> getCompanyById(String id) {
        return buildCompanies().stream()
            .filter(c -> id.equals(c.get("id")))
            .findFirst()
            .orElse(null);
    }

    @Cacheable(value = "companies", key = "'category_' + #category")
    public List<Map<String, Object>> getCompaniesByCategory(String category) {
        return buildCompanies().stream()
            .filter(c -> "all".equals(category) || category.equals(c.get("category")))
            .toList();
    }

    @Cacheable(value = "roles")
    public List<Map<String, Object>> getAllRoles() {
        log.debug("Loading roles into cache");
        return buildRoles();
    }

    @Cacheable(value = "mockTests")
    public List<Map<String, Object>> getAllMockTests() {
        log.debug("Loading mock tests into cache");
        return buildMockTests();
    }

    // ─── Data builders ────────────────────────────────────────────

    private List<Map<String, Object>> buildCompanies() {
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(company("google", "Google", "product",
            "https://www.google.com/favicon.ico", 45, "Hard",
            List.of("Strong problem-solving skills","Proficiency in DSA","System design knowledge","Good communication skills"),
            salary(1200000, 5000000),
            List.of(
                step(1, "Online Assessment", "90 mins", "Coding and problem-solving on HackerRank"),
                step(2, "Phone Screen", "45 mins", "Technical discussion with a Google engineer"),
                step(3, "On-site Round 1", "60 mins", "Data structures and algorithms deep dive"),
                step(4, "On-site Round 2", "60 mins", "System design - design YouTube or WhatsApp"),
                step(5, "On-site Round 3", "45 mins", "Behavioral and Googleyness")
            ),
            List.of("java","python","devops","data-engineer")));

        list.add(company("microsoft", "Microsoft", "product",
            "https://www.microsoft.com/favicon.ico", 32, "Hard",
            List.of("Strong coding skills","Knowledge of algorithms","Team collaboration","Problem-solving mindset"),
            salary(1000000, 4500000),
            List.of(
                step(1, "Online Test", "60 mins", "Coding assessment on Codility"),
                step(2, "Technical Round 1", "60 mins", "DSA and problem-solving"),
                step(3, "Technical Round 2", "60 mins", "System design"),
                step(4, "HR Round", "30 mins", "Behavioral and cultural fit")
            ),
            List.of("dotnet","java","devops","cybersecurity")));

        list.add(company("amazon", "Amazon", "product",
            "https://www.amazon.com/favicon.ico", 60, "Hard",
            List.of("Amazon Leadership Principles","Strong DSA skills","STAR method","Distributed systems knowledge"),
            salary(1100000, 4200000),
            List.of(
                step(1, "Online Assessment", "105 mins", "Two coding problems + work simulation"),
                step(2, "Phone Screen", "60 mins", "LP + technical coding round"),
                step(3, "Virtual Loop Round 1", "60 mins", "Coding + Leadership Principles"),
                step(4, "Virtual Loop Round 2", "60 mins", "System design + LP"),
                step(5, "Bar Raiser Round", "60 mins", "Deep dive into Leadership Principles")
            ),
            List.of("java","python","devops","data-engineer")));

        list.add(company("meta", "Meta", "product",
            "https://www.meta.com/favicon.ico", 28, "Hard",
            List.of("LeetCode hard problem skills","Distributed systems","Move fast culture mindset","Strong communication"),
            salary(1300000, 5500000),
            List.of(
                step(1, "Recruiter Screen", "30 mins", "Initial conversation and background check"),
                step(2, "Technical Phone Screen", "60 mins", "Two coding problems on CoderPad"),
                step(3, "Onsite Coding 1", "60 mins", "Two medium/hard LeetCode-style problems"),
                step(4, "Onsite Coding 2", "60 mins", "Two medium/hard LeetCode-style problems"),
                step(5, "System Design", "60 mins", "Design Instagram Feed or Messenger"),
                step(6, "Behavioral", "45 mins", "Meta values and past experiences")
            ),
            List.of("java","python","mobile")));

        list.add(company("adobe", "Adobe", "product",
            "https://www.adobe.com/favicon.ico", 22, "Medium",
            List.of("Strong DSA fundamentals","Object-oriented design","Creative problem-solving","Relevant experience"),
            salary(800000, 3000000),
            List.of(
                step(1, "Online Coding Test", "75 mins", "HackerEarth platform - DSA problems"),
                step(2, "Technical Round 1", "60 mins", "DSA and project discussion"),
                step(3, "Technical Round 2", "60 mins", "Low-level design / object design"),
                step(4, "Hiring Manager Round", "45 mins", "Team fit and experience mapping")
            ),
            List.of("java","python","testing","mobile")));

        list.add(company("oracle", "Oracle", "product",
            "https://www.oracle.com/favicon.ico", 35, "Medium",
            List.of("Strong Java and SQL skills","Database design expertise","OOP concepts","Cloud fundamentals"),
            salary(700000, 2500000),
            List.of(
                step(1, "Written Test", "60 mins", "Aptitude + technical MCQ"),
                step(2, "Technical Round 1", "60 mins", "Java/SQL coding and OOP"),
                step(3, "Technical Round 2", "45 mins", "Database design and optimization"),
                step(4, "HR Round", "30 mins", "Offer negotiation and cultural fit")
            ),
            List.of("java","dotnet","data-engineer","sap")));

        list.add(company("tcs", "TCS", "service",
            "https://www.tcs.com/favicon.ico", 150, "Easy",
            List.of("Good academic record (60%+)","Basic programming knowledge","Communication skills","Willingness to learn"),
            salary(350000, 800000),
            List.of(
                step(1, "TCS NQT", "90 mins", "Quantitative, Logical, Verbal + Coding"),
                step(2, "Technical Interview", "45 mins", "Core subjects and basics"),
                step(3, "Managerial Round", "30 mins", "Project discussion and team fit"),
                step(4, "HR Interview", "20 mins", "Background verification")
            ),
            List.of("java","dotnet","testing","sap","salesforce")));

        list.add(company("infosys", "Infosys", "service",
            "https://www.infosys.com/favicon.ico", 120, "Easy",
            List.of("Basic programming in any language","Good aptitude score","Team player","Flexibility for relocation"),
            salary(330000, 750000),
            List.of(
                step(1, "InfyTQ Certification", "120 mins", "Online aptitude + programming test"),
                step(2, "Technical Interview", "45 mins", "Programming concepts and projects"),
                step(3, "HR Interview", "20 mins", "Salary, location, and joining discussion")
            ),
            List.of("java","dotnet","sap","testing")));

        list.add(company("wipro", "Wipro", "service",
            "https://www.wipro.com/favicon.ico", 200, "Easy",
            List.of("Consistent academic background","Any programming language basics","Good communication","No active backlogs"),
            salary(320000, 700000),
            List.of(
                step(1, "NLTH Test", "60 mins", "Aptitude + written communication + coding"),
                step(2, "Technical Interview", "45 mins", "Core concepts and aptitude discussion"),
                step(3, "HR Round", "20 mins", "Offer and joining formalities")
            ),
            List.of("java","dotnet","sap","testing","salesforce")));

        list.add(company("hcl", "HCL Technologies", "service",
            "https://www.hcltech.com/favicon.ico", 180, "Easy",
            List.of("Minimum 60% throughout academics","Basic programming","Good communication skills","Eagerness to learn"),
            salary(340000, 780000),
            List.of(
                step(1, "Online Test", "60 mins", "Aptitude, logical reasoning, and verbal"),
                step(2, "Group Discussion", "20 mins", "Communication and group thinking"),
                step(3, "Technical Interview", "40 mins", "Project and tech stack discussion"),
                step(4, "HR Interview", "20 mins", "Final round")
            ),
            List.of("java","dotnet","sap","devops","testing")));

        list.add(company("cognizant", "Cognizant", "service",
            "https://www.cognizant.com/favicon.ico", 130, "Easy",
            List.of("CGPA 7.5+ preferred for GenC Next","Programming knowledge","Logical thinking","Flexible for different time zones"),
            salary(400000, 900000),
            List.of(
                step(1, "GenC / GenC Next Test", "75 mins", "Aptitude + coding + communication"),
                step(2, "Technical Interview", "45 mins", "CS fundamentals and project work"),
                step(3, "HR Interview", "20 mins", "Background and offer discussion")
            ),
            List.of("java","salesforce","sap","testing","dotnet")));

        list.add(company("capgemini", "Capgemini", "service",
            "https://www.capgemini.com/favicon.ico", 100, "Easy",
            List.of("No standing backlogs","Good logical reasoning","Communication skills","Teamwork mindset"),
            salary(380000, 850000),
            List.of(
                step(1, "Pseudo Code Test", "45 mins", "Logical flow and problem-solving"),
                step(2, "Behavioral Assessment", "30 mins", "Video-based behavioral round"),
                step(3, "Technical Interview", "45 mins", "CS basics and project work"),
                step(4, "HR Interview", "20 mins", "Final formalities")
            ),
            List.of("java","dotnet","sap","devops")));

        list.add(company("razorpay", "Razorpay", "startup",
            "https://razorpay.com/favicon.ico", 12, "Medium",
            List.of("Startup mindset","Fast learner","Strong technical skills","Ownership attitude"),
            salary(800000, 2500000),
            List.of(
                step(1, "Coding Round", "90 mins", "Problem-solving on HackerEarth"),
                step(2, "Technical Discussion", "60 mins", "Project deep dive + algorithms"),
                step(3, "System Design", "60 mins", "Design a payment gateway or billing engine"),
                step(4, "Cultural Fit", "30 mins", "Values and startup alignment")
            ),
            List.of("java","python","devops","data-engineer")));

        list.add(company("zepto", "Zepto", "startup",
            "https://zeptonow.com/favicon.ico", 8, "Hard",
            List.of("High-performance engineering mindset","Strong system design skills","High-scale experience","Bias for action"),
            salary(1000000, 3500000),
            List.of(
                step(1, "Online Coding Test", "60 mins", "DSA questions + practical challenge"),
                step(2, "Tech Interview 1", "60 mins", "DSA and problem-solving depth"),
                step(3, "Tech Interview 2", "60 mins", "System design for logistics scale"),
                step(4, "Founder/VP Round", "30 mins", "Vision alignment and culture fit")
            ),
            List.of("java","python","devops","mobile")));

        list.add(company("cred", "CRED", "startup",
            "https://cred.club/favicon.ico", 6, "Hard",
            List.of("High-quality code standards","Product thinking","Strong architectural understanding","Member-first mindset"),
            salary(900000, 3000000),
            List.of(
                step(1, "Coding Assignment", "3 days", "Take-home project to build a working feature"),
                step(2, "Code Review Round", "60 mins", "Walk through your assignment code"),
                step(3, "System Design", "60 mins", "Scale the assignment to production"),
                step(4, "Culture and Values", "45 mins", "CRED values and team fit")
            ),
            List.of("java","python","mobile","devops")));

        list.add(company("groww", "Groww", "startup",
            "https://groww.in/favicon.ico", 10, "Hard",
            List.of("Finance domain interest preferred","Strong backend or fullstack skills","High ownership culture","User-first thinking"),
            salary(900000, 3200000),
            List.of(
                step(1, "Online Coding Round", "75 mins", "2-3 DSA problems on competitive platforms"),
                step(2, "Technical Interview 1", "60 mins", "Algorithms, OS, database concepts"),
                step(3, "Technical Interview 2", "60 mins", "System design + fintech-specific scenarios"),
                step(4, "Bar Raiser / Culture Fit", "30 mins", "Mission-driven assessment")
            ),
            List.of("java","python","data-engineer","devops")));

        list.add(company("swiggy", "Swiggy", "startup",
            "https://swiggy.com/favicon.ico", 20, "Medium",
            List.of("Passion for food tech / logistics","Strong algorithms background","Distributed systems ability","Quick learner"),
            salary(800000, 2800000),
            List.of(
                step(1, "Online Test", "90 mins", "Aptitude + 2 coding questions"),
                step(2, "DSA Round", "60 mins", "Algorithmic problem-solving"),
                step(3, "System Design", "60 mins", "Design Swiggy delivery allocation system"),
                step(4, "Behavioral + Closing", "30 mins", "Past experience + offer")
            ),
            List.of("java","python","devops","data-engineer","mobile")));

        list.add(company("phonepe", "PhonePe", "startup",
            "https://phonepe.com/favicon.ico", 14, "Hard",
            List.of("Fintech passion preferred","High-availability systems experience","Strong Java or Go skills","Security and compliance focus"),
            salary(900000, 3000000),
            List.of(
                step(1, "Coding Round", "90 mins", "HackerEarth - medium to hard problems"),
                step(2, "Technical Round 1", "60 mins", "DSA, system concepts, DBMS"),
                step(3, "Technical Round 2", "60 mins", "System design for payments infra"),
                step(4, "Culture Fit + HR", "30 mins", "Values and compensation discussion")
            ),
            List.of("java","devops","data-engineer","cybersecurity")));

        list.add(company("meesho", "Meesho", "startup",
            "https://meesho.com/favicon.ico", 15, "Medium",
            List.of("Entrepreneurial mindset","Resilience and grit","Strong coding fundamentals","E-commerce domain curiosity"),
            salary(700000, 2800000),
            List.of(
                step(1, "Coding Test", "90 mins", "HackerRank - 3 coding problems"),
                step(2, "Technical Round 1", "60 mins", "DSA + past project discussion"),
                step(3, "Technical Round 2", "60 mins", "System design for e-commerce at scale"),
                step(4, "Leadership Round", "30 mins", "Entrepreneurial mindset evaluation")
            ),
            List.of("java","python","mobile","devops","data-engineer")));

        return list;
    }

    private List<Map<String, Object>> buildRoles() {
        return List.of(
            role("java", "Java Developer", "Code", "Full Stack, Backend, Frontend Development",
                List.of("Full Stack","Backend","Frontend"), "#f89820",
                List.of(
                    res("Java Docs", "https://docs.oracle.com/en/java/", "docs"),
                    res("Spring Boot", "https://spring.io/projects/spring-boot", "docs"),
                    res("Baeldung", "https://www.baeldung.com/", "article"),
                    res("LeetCode Java", "https://leetcode.com/problemset/", "practice"),
                    res("Java Course – FCC", "https://www.youtube.com/watch?v=GoXwIVyNvX0", "video")
                )),
            role("dotnet", ".NET Developer", "Code2", "Full Stack, Backend, Frontend Development",
                List.of("Full Stack","Backend","Frontend"), "#512bd4",
                List.of(
                    res(".NET Docs", "https://learn.microsoft.com/en-us/dotnet/", "docs"),
                    res("C# Guide", "https://learn.microsoft.com/en-us/dotnet/csharp/", "docs"),
                    res("MS Learn .NET", "https://learn.microsoft.com/en-us/training/dotnet/", "course"),
                    res("C# Tutorial – FCC", "https://www.youtube.com/watch?v=GhQdlIFylQ8", "video")
                )),
            role("sap", "SAP Consultant", "Database", "MM, PTP, BTP, Sales, Integration",
                List.of("MM","PTP","BTP","Sales","Integration"), "#008fd3",
                List.of(
                    res("SAP Learning Hub", "https://learning.sap.com/", "course"),
                    res("SAP Help Portal", "https://help.sap.com/", "docs"),
                    res("SAP Community", "https://community.sap.com/", "article"),
                    res("SAP BTP Tutorials", "https://developers.sap.com/tutorial-navigator.html", "course")
                )),
            role("salesforce", "Salesforce Developer", "Cloud", "CRM, Development, Administration",
                List.of("Developer","Admin","Architect"), "#00a1e0",
                List.of(
                    res("Trailhead", "https://trailhead.salesforce.com/", "course"),
                    res("Apex Docs", "https://developer.salesforce.com/docs/atlas.en-us.apexcode.meta/apexcode/", "docs"),
                    res("LWC Docs", "https://developer.salesforce.com/docs/component-library/documentation/en/lwc", "docs"),
                    res("Focus on Force", "https://focusonforce.com/", "course")
                )),
            role("testing", "QA/Testing", "TestTube2", "Manual, Automation, Performance Testing",
                List.of("Manual","Automation","Performance"), "#10b981",
                List.of(
                    res("Selenium Docs", "https://www.selenium.dev/documentation/", "docs"),
                    res("Playwright Docs", "https://playwright.dev/docs/intro", "docs"),
                    res("ISTQB Syllabus", "https://www.istqb.org/certifications/certified-tester-foundation-level", "docs"),
                    res("QA Automation – FCC", "https://www.youtube.com/watch?v=nu1E0sE3QFo", "video")
                )),
            role("devops", "DevOps Engineer", "Settings", "CI/CD, Cloud, Infrastructure as Code",
                List.of("AWS","Azure","GCP","Multi-Cloud"), "#ff6b35",
                List.of(
                    res("AWS Skill Builder", "https://explore.skillbuilder.aws/learn", "course"),
                    res("Kubernetes Docs", "https://kubernetes.io/docs/home/", "docs"),
                    res("Docker Docs", "https://docs.docker.com/", "docs"),
                    res("DevOps Roadmap", "https://roadmap.sh/devops", "article"),
                    res("Terraform Tutorials", "https://developer.hashicorp.com/terraform/tutorials", "course")
                )),
            role("data-engineer", "Data Engineer", "BarChart", "ETL Pipelines, Big Data, Analytics Engineering",
                List.of("ETL/ELT","Big Data","Analytics","ML Ops"), "#8b5cf6",
                List.of(
                    res("Spark Docs", "https://spark.apache.org/docs/latest/", "docs"),
                    res("dbt Docs", "https://docs.getdbt.com/", "docs"),
                    res("DE Zoomcamp", "https://github.com/DataTalksClub/data-engineering-zoomcamp", "course"),
                    res("Kaggle SQL", "https://www.kaggle.com/learn/advanced-sql", "course")
                )),
            role("python", "Python Developer", "Code", "Web, Data Science, Automation, AI/ML",
                List.of("Django/Flask","Data Science","Automation","AI/ML"), "#3776ab",
                List.of(
                    res("Python Docs", "https://docs.python.org/3/", "docs"),
                    res("Real Python", "https://realpython.com/", "article"),
                    res("FastAPI Docs", "https://fastapi.tiangolo.com/", "docs"),
                    res("Kaggle Python", "https://www.kaggle.com/learn/python", "course"),
                    res("Python – FCC", "https://www.youtube.com/watch?v=rfscVS0vtbw", "video")
                )),
            role("mobile", "Mobile Developer", "Smartphone", "iOS, Android, React Native, Flutter",
                List.of("iOS (Swift)","Android (Kotlin)","React Native","Flutter"), "#06b6d4",
                List.of(
                    res("Android Courses", "https://developer.android.com/courses", "course"),
                    res("Flutter Docs", "https://docs.flutter.dev/", "docs"),
                    res("React Native Docs", "https://reactnative.dev/docs/getting-started", "docs"),
                    res("100 Days SwiftUI", "https://www.hackingwithswift.com/100/swiftui", "course")
                )),
            role("cybersecurity", "Cybersecurity Analyst", "Shield", "SOC, Penetration Testing, Cloud Security",
                List.of("SOC Analyst","Penetration Testing","Cloud Security","AppSec"), "#ef4444",
                List.of(
                    res("TryHackMe", "https://tryhackme.com/", "practice"),
                    res("Hack The Box", "https://www.hackthebox.com/", "practice"),
                    res("OWASP Top 10", "https://owasp.org/www-project-top-ten/", "docs"),
                    res("Security+ – Prof Messer", "https://www.professormesser.com/security-plus/sy0-701/sy0-701-video/sy0-701-comptia-security-plus-course/", "course")
                ))
        );
    }

    private List<Map<String, Object>> buildMockTests() {
        return List.of(
            mockTest("mock-1","Complete Interview Mock Test",120,50,"Medium","free",List.of("Aptitude","Coding","Technical")),
            mockTest("mock-2","Coding Assessment",90,4,"Hard","free",List.of("Coding")),
            mockTest("mock-3","Aptitude Test",60,30,"Easy","free",List.of("Aptitude")),
            mockTest("mock-4","System Design Mock",60,3,"Hard","paid",List.of("System Design")),
            mockTest("mock-5","TCS NQT Simulator",90,40,"Easy","free",List.of("Aptitude","Verbal","Coding")),
            mockTest("mock-6","Amazon Leadership Principles",45,20,"Medium","free",List.of("Behavioral"))
        );
    }

    // ─── Builder helpers ──────────────────────────────────────────

    private Map<String, Object> company(String id, String name, String category,
                                        String logo, int openPositions, String difficulty,
                                        List<String> requirements, Map<String, Object> salary,
                                        List<Map<String, Object>> hiringProcess,
                                        List<String> rolesHiring) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", name); m.put("category", category);
        m.put("logo", logo); m.put("openPositions", openPositions);
        m.put("difficulty", difficulty); m.put("requirements", requirements);
        m.put("salaryRange", salary); m.put("hiringProcess", hiringProcess);
        m.put("rolesHiring", rolesHiring);
        return m;
    }

    private Map<String, Object> salary(int min, int max) {
        return Map.of("min", min, "max", max, "currency", "INR");
    }

    private Map<String, Object> step(int step, String name, String duration, String description) {
        return Map.of("step", step, "name", name, "duration", duration, "description", description);
    }

    private Map<String, Object> role(String id, String name, String icon, String description,
                                     List<String> variants, String color,
                                     List<Map<String, Object>> resources) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", name); m.put("icon", icon);
        m.put("description", description); m.put("variants", variants);
        m.put("color", color); m.put("resources", resources);
        return m;
    }

    private Map<String, Object> res(String name, String url, String type) {
        return Map.of("name", name, "url", url, "type", type);
    }

    private Map<String, Object> mockTest(String id, String title, int duration, int questions,
                                         String difficulty, String type, List<String> sections) {
        return Map.of("id",id,"title",title,"duration",duration,"questions",questions,
                      "difficulty",difficulty,"type",type,"sections",sections);
    }
}
