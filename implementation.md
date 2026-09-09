# 366PI Recruitment Platform
# Implementation Guide — Eureka Server + Spring Boot Admin + User Service

## 1. Goal

This document describes the first implementation phase of the 366PI Recruitment Platform.

For this phase we will build only:

1. `discovery-server` — Eureka Server
2. `admin-server` — Spring Boot Admin Server
3. `user-service` — Candidate profile service

The target local architecture is:

```text
                         366PI Recruitment Platform
                                  |
                                  |
                    +-------------+-------------+
                    |                           |
                    v                           v
             Eureka Server              Spring Boot Admin
                :8761                        :8082
                    ^                           ^
                    |                           |
             registers                    discovers/monitors
                    |                           |
                    +-------------+-------------+
                                  |
                                  v
                           User Service
                              :8081
                                  |
                                  v
                             PostgreSQL
                              user_db
```

The frontend and API Gateway are intentionally NOT implemented in this phase.

---

# 2. Technology Baseline

Use:

- Java 17
- Spring Boot 3.5.x
- Spring Cloud 2025.0.x
- Maven
- PostgreSQL
- Spring Cloud Netflix Eureka
- Spring Boot Admin
- Spring Data JPA
- Hibernate
- Flyway
- Spring Boot Actuator
- Bean Validation
- Lombok
- JUnit 5
- Mockito
- Testcontainers later

Recommended baseline for this document:

```text
Spring Boot       3.5.15
Spring Cloud      2025.0.3
Spring Boot Admin 3.5.x
Java              17
Maven             3.9+
PostgreSQL        16+
```

Spring Cloud 2025.0.x is the Spring Cloud release train for Spring Boot 3.5.x.

Do not mix Spring Boot 4.x dependencies with Spring Cloud 2025.0.x.

---

# 3. Repository Structure

Create one Git repository:

```text
366pi-recruitment/
|
+-- discovery-server/
|
+-- admin-server/
|
+-- user-service/
|
+-- docs/
|
+-- README.md
|
+-- .gitignore
```

Later the repository can grow into:

```text
366pi-recruitment/
|
+-- api-gateway/
+-- discovery-server/
+-- config-server/
+-- auth-service/
+-- user-service/
+-- job-service/
+-- application-service/
+-- interview-service/
+-- notification-service/
+-- admin-service/
+-- admin-server/
|
+-- docker/
+-- k8s/
+-- docs/
```

---

# 4. Service Ports

Keep fixed local ports initially.

| Service | Port |
|---|---:|
| Eureka Server | 8761 |
| User Service | 8081 |
| Spring Boot Admin | 8082 |
| PostgreSQL | 5432 |

Later the API Gateway can use:

```text
8080
```

---

# 5. Project 1 — Eureka Discovery Server

## 5.1 Create Spring Starter Project

In Spring Tool Suite / Spring Initializr:

```text
Name:        discovery-server
Group:       com.pi
Artifact:    discovery-server
Package:     com.pi.discovery
Java:        17
Packaging:   Jar
Language:    Java
```

Dependencies:

```text
Spring Web
Eureka Server
Spring Boot Actuator
```

---

# 6. Eureka Server pom.xml

Use this structure:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.15</version>
        <relativePath/>
    </parent>

    <groupId>com.pi</groupId>
    <artifactId>discovery-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <name>discovery-server</name>
    <description>366PI Eureka Discovery Server</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2025.0.3</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

---

# 7. Eureka Application Class

Create:

```text
src/main/java/com/pi/discovery/DiscoveryServerApplication.java
```

```java
package com.pi.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
```

---

# 8. Eureka application.yml

Create:

```text
src/main/resources/application.yml
```

```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false

  server:
    enable-self-preservation: true

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
```

Why:

```text
register-with-eureka: false
```

Because the Eureka server should not register itself as a normal application.

```text
fetch-registry: false
```

Because this single local registry does not need to fetch another registry.

---

# 9. Start Eureka

From the discovery-server directory:

```bash
mvn spring-boot:run
```

Or:

```bash
mvn clean package
java -jar target/discovery-server-0.0.1-SNAPSHOT.jar
```

Open:

```text
http://localhost:8761
```

You should see the Eureka dashboard.

At this point:

```text
Eureka Server
      |
      +-- No registered services yet
```

That is expected.

---

# 10. Project 2 — Spring Boot Admin Server

Spring Boot Admin is different from your future 366PI business Admin Service.

Spring Boot Admin is for technical monitoring:

```text
Health
Metrics
Environment
Logs
Threads
JVM
Actuator
Application status
```

Your future `admin-service` will handle:

```text
Jobs
Candidates
Applications
Interviews
Recruitment operations
```

Do not combine these two responsibilities.

---

# 11. Create Admin Server

Create Spring Starter Project:

```text
Name:        admin-server
Group:       com.pi
Artifact:    admin-server
Package:     com.pi.admin
Java:        17
Packaging:   Jar
Language:    Java
```

Dependencies:

```text
Spring Web
Spring Boot Admin Server
Eureka Discovery Client
Spring Boot Actuator
```

---

# 12. Admin Server pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.15</version>
        <relativePath/>
    </parent>

    <groupId>com.pi</groupId>
    <artifactId>admin-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <name>admin-server</name>
    <description>366PI Spring Boot Admin Server</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2025.0.3</spring-cloud.version>
        <spring-boot-admin.version>3.5.10</spring-boot-admin.version>
    </properties>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>de.codecentric</groupId>
                <artifactId>spring-boot-admin-dependencies</artifactId>
                <version>${spring-boot-admin.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-server</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

---

# 13. Admin Server Application Class

Create:

```text
src/main/java/com/pi/admin/AdminServerApplication.java
```

```java
package com.pi.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import de.codecentric.boot.admin.server.config.EnableAdminServer;

@SpringBootApplication
@EnableAdminServer
public class AdminServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
```

---

# 14. Admin Server application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: admin-server

  boot:
    admin:
      discovery:
        enabled: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
```

The important part is:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

The Admin Server becomes a Eureka client and can discover applications registered with Eureka.

---

# 15. Start Admin Server

Start Eureka first.

Then:

```bash
mvn spring-boot:run
```

Open:

```text
http://localhost:8082
```

Initially there may be no monitored application.

That is expected because User Service has not been created yet.

---

# 16. Project 3 — User Service

This is the first actual business service.

Responsibilities:

```text
User Service
|
+-- Candidate profile
+-- Candidate contact information
+-- Candidate location
+-- Candidate skills
+-- Candidate education
+-- Candidate profile status
```

It does NOT own:

```text
Password
JWT
Login
Job
Application
Interview
```

Authentication will later belong to `auth-service`.

---

# 17. Create User Service

Spring Starter Project:

```text
Name:        user-service
Group:       com.pi
Artifact:    user-service
Package:     com.pi.user
Java:        17
Packaging:   Jar
Language:    Java
```

Dependencies:

```text
Spring Web
Spring Data JPA
PostgreSQL Driver
Validation
Lombok
Eureka Discovery Client
Spring Boot Actuator
Flyway
Spring Boot Test
```

---

# 18. User Service pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.15</version>
        <relativePath/>
    </parent>

    <groupId>com.pi</groupId>
    <artifactId>user-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <name>user-service</name>
    <description>366PI Candidate User Service</description>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2025.0.3</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

        </dependencies>
    </dependencyManagement>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>

            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

        </plugins>
    </build>

</project>
```

---

# 19. PostgreSQL Database

Create database:

```sql
CREATE DATABASE user_db;
```

Recommended development credentials:

```text
Database: user_db
Username: postgres
Password: your-local-password
Port:     5432
```

Do not commit the real password into Git.

For production use environment variables or a secrets manager.

---

# 20. User Service application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service

  datasource:
    url: jdbc:postgresql://localhost:5432/user_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
```

Important:

```yaml
ddl-auto: validate
```

Do not use:

```yaml
ddl-auto: create
```

or:

```yaml
ddl-auto: update
```

for a production-style project.

Flyway owns schema changes.

---

# 21. User Service Package Structure

Use:

```text
src/main/java/com/pi/user/

├── UserServiceApplication.java
│
├── controller/
│   └── UserController.java
│
├── service/
│   ├── UserService.java
│   └── UserServiceImpl.java
│
├── repository/
│   └── UserRepository.java
│
├── entity/
│   └── User.java
│
├── dto/
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   └── UserResponse.java
│
├── mapper/
│   └── UserMapper.java
│
├── exception/
│   ├── UserNotFoundException.java
│   └── GlobalExceptionHandler.java
│
└── config/
    └── JacksonConfig.java
```

Do not expose JPA entities directly from controllers.

---

# 22. User Service Application Class

```java
package com.pi.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

No special Eureka annotation is required for the modern Eureka client starter.

The Eureka client auto-configuration registers the service.

---

# 23. Database Migration

Create:

```text
src/main/resources/db/migration/V1__create_users.sql
```

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    auth_user_id UUID UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(30),
    location VARCHAR(255),
    headline VARCHAR(255),
    bio VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_users_auth_user_id
    ON users(auth_user_id);
```

---

# 24. User Status

Create:

```java
package com.pi.user.entity;

public enum ProfileStatus {

    ACTIVE,
    INACTIVE,
    SUSPENDED
}
```

---

# 25. User Entity

```java
package com.pi.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_auth_user_id", columnList = "auth_user_id")
    }
)
@Getter
@Setter
public class User {

    @Id
    private UUID id;

    @Column(name = "auth_user_id", unique = true)
    private UUID authUserId;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String location;

    private String headline;

    @Column(length = 2000)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
```

---

# 26. Why UUID?

Use UUID for service-facing identifiers:

```text
550e8400-e29b-41d4-a716-446655440000
```

Benefits:

- Avoids predictable sequential IDs
- Works well across distributed systems
- Easier future data migration
- No central ID generator required

Do not expose database implementation details.

---

# 27. Repository

```java
package com.pi.user.repository;

import com.pi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByAuthUserId(UUID authUserId);
}
```

---

# 28. Request DTO

```java
package com.pi.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 255)
        String location,

        @Size(max = 255)
        String headline,

        @Size(max = 2000)
        String bio
) {
}
```

---

# 29. Response DTO

```java
package com.pi.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID authUserId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String location,
        String headline,
        String bio,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
```

---

# 30. Mapper

For a small service, a manual mapper is completely acceptable.

```java
package com.pi.user.mapper;

import com.pi.user.dto.UserResponse;
import com.pi.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getAuthUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getLocation(),
                user.getHeadline(),
                user.getBio(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
```

Do not add MapStruct just because it is popular. Introduce it when mapping complexity actually justifies it.

---

# 31. Service Interface

```java
package com.pi.user.service;

import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUser(UUID id);
}
```

---

# 32. Service Implementation

```java
package com.pi.user.service;

import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;
import com.pi.user.entity.ProfileStatus;
import com.pi.user.entity.User;
import com.pi.user.exception.UserNotFoundException;
import com.pi.user.mapper.UserMapper;
import com.pi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();

        User user = new User();

        user.setId(UUID.randomUUID());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setLocation(request.location());
        user.setHeadline(request.headline());
        user.setBio(request.bio());
        user.setStatus(ProfileStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse getUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found: " + id));

        return userMapper.toResponse(user);
    }
}
```

---

# 33. Exception

```java
package com.pi.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
```

---

# 34. Global Exception Handler

```java
package com.pi.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(
            UserNotFoundException ex) {

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "USER_NOT_FOUND",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex) {

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage()
        );

        return ResponseEntity.badRequest().body(error);
    }
}
```

Create:

```java
package com.pi.user.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message
) {
}
```

---

# 35. Controller

```java
package com.pi.user.controller;

import com.pi.user.dto.CreateUserRequest;
import com.pi.user.dto.UserResponse;
import com.pi.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request) {

        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(
            @PathVariable UUID id) {

        return userService.getUser(id);
    }
}
```

---

# 36. First User Service API

After implementation:

### Create user

```http
POST http://localhost:8081/api/v1/users
Content-Type: application/json
```

Body:

```json
{
  "firstName": "Sanchit",
  "lastName": "Kumar",
  "email": "sanchit@example.com",
  "phone": "9999999999",
  "location": "Ranchi",
  "headline": "Java Backend Developer",
  "bio": "Software engineer interested in Spring Boot and microservices."
}
```

Expected:

```text
HTTP 201 CREATED
```

---

# 37. Get User

```http
GET http://localhost:8081/api/v1/users/{id}
```

Expected:

```json
{
  "id": "UUID",
  "authUserId": null,
  "firstName": "Sanchit",
  "lastName": "Kumar",
  "email": "sanchit@example.com",
  "phone": "9999999999",
  "location": "Ranchi",
  "headline": "Java Backend Developer",
  "bio": "...",
  "status": "ACTIVE"
}
```

---

# 38. Register User Service with Eureka

Once User Service starts:

```text
User Service
     |
     | registration
     v
Eureka Server :8761
```

Eureka should show:

```text
APPLICATION

USER-SERVICE
    UP (1)
```

The service ID is derived from:

```yaml
spring:
  application:
    name: user-service
```

Do not randomly change this after other services start depending on it.

---

# 39. User Service in Spring Boot Admin

The final local flow is:

```text
User Service
     |
     | Eureka registration
     v
Eureka Server
     ^
     |
     | service discovery
     |
Admin Server
     |
     v
Spring Boot Admin UI
```

Open:

```text
http://localhost:8082
```

The User Service should appear automatically because the Admin Server is using Eureka discovery.

Spring Boot Admin can discover applications through Spring Cloud DiscoveryClient, including Eureka.

---

# 40. Actuator Endpoints

Test:

```text
http://localhost:8081/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

Test:

```text
http://localhost:8081/actuator/info
```

and:

```text
http://localhost:8081/actuator/metrics
```

These endpoints are what allow operational monitoring.

---

# 41. Start Order

Always start during local development in this order:

```text
1. PostgreSQL
        |
        v
2. Eureka Server :8761
        |
        v
3. User Service :8081
        |
        v
4. Admin Server :8082
```

Admin Server can technically start before User Service, but for predictable local testing use the above order.

---

# 42. Verify the complete setup

## Step 1

Start PostgreSQL.

Check:

```text
5432
```

## Step 2

Start Eureka.

Open:

```text
http://localhost:8761
```

Expected:

```text
Eureka Dashboard
```

## Step 3

Start User Service.

Check:

```text
http://localhost:8081/actuator/health
```

Expected:

```json
{"status":"UP"}
```

## Step 4

Go back to:

```text
http://localhost:8761
```

Expected:

```text
USER-SERVICE    UP
```

## Step 5

Start Admin Server.

Open:

```text
http://localhost:8082
```

Expected:

```text
user-service
```

## Step 6

Create a user.

Use Postman:

```http
POST /api/v1/users
```

## Step 7

Verify database.

```sql
SELECT * FROM users;
```

---

# 43. Complete request path

For this phase:

```text
Postman
   |
   v
User Service
   |
   +------> PostgreSQL
   |
   +------> Eureka
   |
   +------> Actuator
                  |
                  v
            Admin Server
```

There is no Gateway yet.

Later it becomes:

```text
Frontend
   |
   v
API Gateway
   |
   v
User Service
   |
   +----> PostgreSQL
   |
   +----> Eureka
   |
   +----> Kafka
```

---

# 44. Important architecture rule

Do NOT make the User Service call Eureka manually.

Bad:

```java
EurekaClient client = ...
```

inside normal business logic.

Eureka is infrastructure.

Business code should not care how service discovery works.

Later, if you use:

```text
OpenFeign
```

the service can communicate using the logical service name.

Example:

```text
USER-SERVICE
JOB-SERVICE
INTERVIEW-SERVICE
```

---

# 45. Important architecture rule — no shared database

Do not create:

```text
366pi_db
```

and make every service use it.

Instead:

```text
user-service       -> user_db
auth-service       -> auth_db
job-service        -> job_db
application-service -> application_db
interview-service  -> interview_db
```

This preserves service ownership.

---

# 46. Important architecture rule — User Service does not own passwords

Do NOT add:

```text
password
passwordHash
loginAttempts
refreshToken
```

to the User entity.

Later:

```text
auth-service
```

will own:

```text
credentials
roles
password hash
account status
refresh tokens
login
JWT
```

User Service owns the profile.

---

# 47. Testing Checklist

Before moving to Auth Service, verify:

```text
[ ] Eureka starts
[ ] Eureka dashboard works
[ ] User Service starts
[ ] User Service registers with Eureka
[ ] PostgreSQL connection works
[ ] Flyway migration works
[ ] users table is created
[ ] POST /api/v1/users works
[ ] GET /api/v1/users/{id} works
[ ] Validation works
[ ] Invalid email is rejected
[ ] Missing firstName is rejected
[ ] Duplicate email is rejected
[ ] 404 user is handled
[ ] Actuator health works
[ ] Admin Server starts
[ ] Admin Server discovers User Service
[ ] User Service appears in Admin UI
```

---

# 48. Git Commits

Do not make one giant commit.

Use:

```text
git checkout -b feature/infrastructure
```

Commit 1:

```text
chore: initialize recruitment repository
```

Commit 2:

```text
feat: add eureka discovery server
```

Commit 3:

```text
feat: add spring boot admin server
```

Commit 4:

```text
feat: initialize user service
```

Commit 5:

```text
feat: add user database migration
```

Commit 6:

```text
feat: implement user profile APIs
```

Commit 7:

```text
feat: add actuator monitoring
```

---

# 49. Definition of Done

This phase is complete only when this works:

```text
                    +----------------+
                    | Eureka :8761   |
                    +-------+--------+
                            ^
                            |
                     registration
                            |
                    +-------+--------+
                    | User :8081    |
                    +-------+--------+
                       |         |
                       |         |
                       v         v
                 PostgreSQL   Actuator
                                |
                                v
                    +-----------+---------+
                    | Admin Server :8082 |
                    +--------------------+
```

And the Eureka dashboard shows:

```text
USER-SERVICE    UP
ADMIN-SERVER    UP
```

The Spring Boot Admin dashboard shows:

```text
USER-SERVICE
```

with health/metrics available.

---

# 50. Next Phase

After this phase, implement:

```text
Phase 2

Auth Service
    |
    +-- Registration
    +-- Password hashing
    +-- Login
    +-- JWT
    +-- Refresh token
    +-- Roles
    +-- Account activation

Then:

Job Service
    |
Application Service
    |
Interview Service
    |
Notification Service
    |
API Gateway
```

Do NOT implement all of these at once.

First make:

```text
Eureka
   +
Admin Server
   +
User Service
   +
PostgreSQL
```

stable and tested.

---

# 51. Production Evolution

The local setup above is intentionally simple.

Production should evolve toward:

```text
Internet
   |
Load Balancer
   |
API Gateway
   |
+------------------------------+
| Microservices                |
|                              |
| Auth                         |
| User                         |
| Job                          |
| Application                  |
| Interview                    |
| Notification                 |
+------------------------------+
   |
Kafka
   |
Observability
|
+-- Prometheus
+-- Grafana
+-- OpenTelemetry
+-- Logs
```

For production, also add:

```text
HTTPS
Secrets Manager
Database backups
Database migrations
Rate limiting
Centralized logs
Distributed tracing
Health checks
Readiness/liveness probes
CI/CD
Container images
Kubernetes
```

---

# 52. Senior Engineering Rules for This Project

Follow these rules from the beginning:

1. Never store plaintext passwords.
2. Never expose JPA entities directly.
3. Never share databases between microservices.
4. Never commit secrets.
5. Use Flyway for schema evolution.
6. Use DTOs at service boundaries.
7. Validate incoming requests.
8. Use UUID identifiers.
9. Keep controllers thin.
10. Put business logic in services.
11. Keep repositories focused on persistence.
12. Use transactions around state-changing operations.
13. Do not make business logic dependent on Eureka.
14. Use Kafka for asynchronous business events.
15. Use Redis only where there is a clear need.
16. Add idempotency to important commands later.
17. Add audit logging for administrative actions.
18. Test business logic independently.
19. Use integration tests against real PostgreSQL/Kafka using Testcontainers.
20. Keep observability as part of the architecture, not as an afterthought.

---

# 53. Useful URLs During Development

Eureka:

```text
http://localhost:8761
```

User Service:

```text
http://localhost:8081
```

User Health:

```text
http://localhost:8081/actuator/health
```

User Metrics:

```text
http://localhost:8081/actuator/metrics
```

Spring Boot Admin:

```text
http://localhost:8082
```

---

# 54. Final target for this implementation

When you finish this document, you should have three independently runnable Spring Boot applications:

```text
366pi-recruitment
|
+-- discovery-server
|       |
|       +-- Eureka
|       +-- :8761
|
+-- admin-server
|       |
|       +-- Spring Boot Admin
|       +-- Eureka Client
|       +-- :8082
|
+-- user-service
        |
        +-- REST API
        +-- PostgreSQL
        +-- JPA
        +-- Flyway
        +-- Eureka Client
        +-- Actuator
        +-- :8081
```

This is the correct foundation before introducing the authentication, jobs, applications, and interview scheduling domains.
