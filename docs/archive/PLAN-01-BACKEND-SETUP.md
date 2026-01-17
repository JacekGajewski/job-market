# Backend Setup Implementation Plan

## Overview
Spring Boot 3.x project with Java 21, Gradle, JPA, and PostgreSQL/H2.

## 1. Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/jobmarket/
│   │   │   ├── JobMarketApplication.java
│   │   │   ├── config/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── controller/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
├── build.gradle
├── settings.gradle
└── gradle/wrapper/
```

## 2. Gradle Configuration (build.gradle)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.jobmarket'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    runtimeOnly 'org.postgresql:postgresql'
    runtimeOnly 'com.h2database:h2'

    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    implementation 'org.jsoup:jsoup:1.18.3'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    developmentOnly 'org.springframework.boot:spring-boot-devtools'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 3. Application Properties

### application.yml
```yaml
spring:
  application:
    name: job-market
  profiles:
    active: dev
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080
```

### application-dev.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:jobmarketdb
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    show-sql: true
```

### application-prod.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:jobmarket}
    username: ${DB_USERNAME:jobmarket}
    password: ${DB_PASSWORD:jobmarket}
```

## 4. JPA Entities

### BaseEntity.java
```java
@Getter
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### JobCountRecord.java
```java
@Entity
@Table(name = "job_count_record")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobCountRecord extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private Integer count;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(nullable = false, length = 100)
    private String location;
}
```

### TrackedCategory.java
```java
@Entity
@Table(name = "tracked_category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrackedCategory extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
```

## 5. Repositories

### JobCountRecordRepository.java
```java
@Repository
public interface JobCountRecordRepository extends JpaRepository<JobCountRecord, Long> {
    List<JobCountRecord> findByCategoryOrderByFetchedAtDesc(String category);
    Optional<JobCountRecord> findFirstByCategoryOrderByFetchedAtDesc(String category);

    @Query("SELECT j FROM JobCountRecord j WHERE j.category = :category " +
           "AND j.fetchedAt >= :startDate ORDER BY j.fetchedAt ASC")
    List<JobCountRecord> findByCategoryAndFetchedAtAfter(
        @Param("category") String category,
        @Param("startDate") LocalDateTime startDate);
}
```

### TrackedCategoryRepository.java
```java
@Repository
public interface TrackedCategoryRepository extends JpaRepository<TrackedCategory, Long> {
    List<TrackedCategory> findByActiveTrue();
    Optional<TrackedCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
```

## 6. Flyway Migrations

### V1__create_initial_schema.sql
```sql
CREATE TABLE tracked_category (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE job_count_record (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(100) NOT NULL,
    count INTEGER NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    location VARCHAR(100) NOT NULL
);

CREATE INDEX idx_job_count_category ON job_count_record(category);
CREATE INDEX idx_job_count_fetched_at ON job_count_record(fetched_at);
```

### V2__seed_initial_categories.sql
```sql
INSERT INTO tracked_category (name, slug, active, created_at) VALUES
    ('Java', 'java', true, CURRENT_TIMESTAMP),
    ('Python', 'python', true, CURRENT_TIMESTAMP),
    ('JavaScript', 'javascript', true, CURRENT_TIMESTAMP),
    ('Angular', 'angular', true, CURRENT_TIMESTAMP),
    ('React', 'react', true, CURRENT_TIMESTAMP);
```

## 7. Main Application

```java
@SpringBootApplication
@EnableScheduling
public class JobMarketApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobMarketApplication.class, args);
    }
}
```

## 8. Implementation Sequence

1. Create directory structure
2. Create build.gradle and settings.gradle
3. Create application.yml files
4. Create BaseEntity, JobCountRecord, TrackedCategory
5. Create repositories
6. Create Flyway migrations
7. Create JobMarketApplication.java
8. Run and verify with H2 console

## Running

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew bootRun
```

Access H2 console at: http://localhost:8080/h2-console
