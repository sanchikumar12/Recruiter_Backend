# 366PI Recruitment Platform — Auth Service Implementation

## 1. Scope

Build the authentication microservice for 366PI.

Auth Service owns:
- Registration
- Login
- Password hashing
- Account activation
- Roles
- JWT access tokens
- Refresh tokens
- Account status
- Password reset/change

Auth Service does NOT own:
- Candidate profile
- Resume
- Skills
- Education
- Jobs
- Applications
- Interviews

Those belong to separate services.

## 2. Architecture

```text
Client
  |
  v
API Gateway :8080
  |
  v
Auth Service :8083
  |
  +---- auth_db (PostgreSQL)
  |
  +---- Eureka :8761
  |
  +---- Kafka (later)
  |
  +---- Notification Service (later)
```

The User Service owns `user_db`; Auth Service must never share that database.

## 3. Technology

Recommended baseline:
- Java 17
- Spring Boot 3.5.x
- Spring Cloud 2025.0.x
- Maven
- PostgreSQL 16+
- Spring Security
- Spring Data JPA
- Flyway
- Eureka Discovery Client
- Spring Boot Actuator
- JJWT
- Lombok
- JUnit 5 / Mockito
- Testcontainers for integration tests

Use mutually compatible Spring Boot/Spring Cloud versions. Do not mix Spring Boot 4.x dependencies with a Spring Cloud release train intended for Boot 3.x.

## 4. Create the Project

```text
Name:        auth-service
Group:       com.pi
Artifact:    auth-service
Package:     com.pi.auth
Java:        17
Packaging:   Jar
Language:    Java
```

Dependencies:
```text
Spring Web
Spring Security
Spring Data JPA
Validation
PostgreSQL Driver
Flyway
Eureka Discovery Client
Spring Boot Actuator
Lombok
JWT library
Spring Boot Test
Spring Security Test
```

## 5. Package Structure

```text
com.pi.auth
├── AuthServiceApplication.java
├── controller
│   └── AuthController.java
├── service
│   ├── AuthService.java
│   ├── AuthServiceImpl.java
│   ├── JwtService.java
│   └── RefreshTokenService.java
├── repository
│   ├── AuthUserRepository.java
│   ├── RefreshTokenRepository.java
│   └── ActivationTokenRepository.java
├── entity
│   ├── AuthUser.java
│   ├── RefreshToken.java
│   └── ActivationToken.java
├── dto
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── RefreshTokenRequest.java
│   ├── AuthResponse.java
│   └── ActivateAccountRequest.java
├── security
│   ├── SecurityConfig.java
│   └── JwtAuthenticationFilter.java
├── enums
│   ├── Role.java
│   └── AccountStatus.java
├── exception
│   ├── GlobalExceptionHandler.java
│   ├── AuthException.java
│   └── InvalidTokenException.java
└── config
    ├── JwtProperties.java
    └── JwtConfig.java
```

## 6. Maven pom.xml

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.15</version>
    <relativePath/>
</parent>

<groupId>com.pi</groupId>
<artifactId>auth-service</artifactId>
<version>0.0.1-SNAPSHOT</version>

<properties>
    <java.version>17</java.version>
    <spring-cloud.version>2025.0.3</spring-cloud.version>
    <jjwt.version>0.12.6</jjwt.version>
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
        <artifactId>spring-boot-starter-security</artifactId>
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
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
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

    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
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
```

## 7. Database

Create:

```sql
CREATE DATABASE auth_db;
```

Never let another microservice directly use this database.

## 8. application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: auth-service

  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
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

jwt:
  secret: ${JWT_SECRET:development-only-use-a-long-random-secret}
  access-token-expiration: 900000
  refresh-token-expiration: 604800000
```

Production secrets must come from environment variables or a secrets manager.

## 9. Application Class

```java
package com.pi.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

## 10. Enums

```java
package com.pi.auth.enums;

public enum Role {
    CANDIDATE,
    ADMIN,
    RECRUITER
}
```

```java
package com.pi.auth.enums;

public enum AccountStatus {
    PENDING_ACTIVATION,
    ACTIVE,
    LOCKED,
    DISABLED
}
```

## 11. Flyway Migration — Auth Users

Create:

```text
src/main/resources/db/migration/V1__create_auth_users.sql
```

```sql
CREATE TABLE auth_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_auth_users_status
    ON auth_users(status);
```

## 12. Refresh Token Migration

Create:

```text
V2__create_refresh_tokens.sql
```

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens(expires_at);
```

## 13. Activation Token Migration

Create:

```text
V3__create_activation_tokens.sql
```

```sql
CREATE TABLE activation_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_activation_tokens_user_id
    ON activation_tokens(user_id);
```

## 14. AuthUser Entity

```java
package com.pi.auth.entity;

import com.pi.auth.enums.AccountStatus;
import com.pi.auth.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
@Getter
@Setter
public class AuthUser {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private Instant lockedUntil;

    private Instant lastLoginAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
```

## 15. RefreshToken Entity

```java
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private Instant createdAt;
}
```

## 16. ActivationToken Entity

```java
@Entity
@Table(name = "activation_tokens")
@Getter
@Setter
public class ActivationToken {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdAt;
}
```

## 17. Repositories

```java
public interface AuthUserRepository
        extends JpaRepository<AuthUser, UUID> {

    Optional<AuthUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
```

```java
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteAllByUserId(UUID userId);
}
```

```java
public interface ActivationTokenRepository
        extends JpaRepository<ActivationToken, UUID> {

    Optional<ActivationToken> findByTokenHash(String tokenHash);
}
```

## 18. DTOs

Registration:

```java
public record RegisterRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {}
```

Login:

```java
public record LoginRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {}
```

Refresh:

```java
public record RefreshTokenRequest(
        @NotBlank
        String refreshToken
) {}
```

Activation:

```java
public record ActivateAccountRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {}
```

Response:

```java
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
```

## 19. Password Security

Create one `PasswordEncoder` bean:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Never:
- Store plaintext passwords
- Encrypt passwords expecting to decrypt them later
- Implement your own hashing algorithm
- Return password hashes from an API

## 20. JWT Properties

```java
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {}
```

Enable it:

```java
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
```

## 21. JWT Service

```java
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;

        this.key = Keys.hmacShaKeyFor(
                properties.secret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(AuthUser user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusMillis(
                                        properties.accessTokenExpiration()
                                )
                        )
                )
                .signWith(key)
                .compact();
    }

    public Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {

        try {
            Claims claims = extractClaims(token);

            return claims.getExpiration()
                    .after(new Date());

        } catch (Exception ex) {
            return false;
        }
    }
}
```

JWT should contain only small, non-sensitive claims:

```json
{
  "sub": "user-uuid",
  "email": "candidate@example.com",
  "role": "CANDIDATE",
  "iat": 1757320000,
  "exp": 1757320900
}
```

Never put passwords, resumes, phone numbers, or large profile data into JWT.

## 22. Security Configuration

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/activate",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## 23. JWT Authentication Filter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header =
                request.getHeader("Authorization");

        if (header == null ||
                !header.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.extractClaims(token);

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        var authority =
                new SimpleGrantedAuthority("ROLE_" + role);

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(authority)
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
```

## 24. Registration

Endpoint:

```http
POST /api/v1/auth/register
```

Request:

```json
{
  "email": "candidate@example.com",
  "password": "StrongPassword123!"
}
```

Server behavior:

```text
Validate request
     |
Check email uniqueness
     |
Hash password with BCrypt
     |
Create AuthUser
     |
Assign CANDIDATE role
     |
PENDING_ACTIVATION
     |
Create activation token
```

The public registration endpoint must never accept:

```json
{
  "role": "ADMIN"
}
```

The role is controlled by the server.

## 25. Login

Endpoint:

```http
POST /api/v1/auth/login
```

Flow:

```text
Email + Password
      |
      v
Find AuthUser
      |
      v
Check account status
      |
      v
BCrypt.matches()
      |
      v
Create access JWT
      |
      v
Create refresh token
      |
      v
Return AuthResponse
```

For invalid email or password return the same message:

```text
Invalid credentials
```

Do not reveal whether an email exists.

## 26. Refresh Tokens

Recommended lifetime:

```text
Access Token  -> 15 minutes
Refresh Token -> 7 days
```

Refresh tokens should be:
- Random
- Stored as hashes
- Revocable
- Rotated after use

Recommended rotation:

```text
Refresh Token A
      |
      v
POST /refresh
      |
      +--> revoke A
      |
      +--> create B
      |
      v
New Access Token + Refresh Token B
```

## 27. Activation

Do not send a permanent plaintext password by email.

Recommended flow:

```text
Register
   |
   v
PENDING_ACTIVATION
   |
   v
Generate random activation token
   |
   v
Store token hash
   |
   v
Notification Service
   |
   v
Email
   |
   v
User activates account
   |
   v
Set password
   |
   v
ACTIVE
```

Activation tokens should:
- Expire
- Be single-use
- Be stored as hashes
- Never be logged

## 28. Auth Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        return authService.refresh(request);
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(
            @Valid @RequestBody ActivateAccountRequest request) {

        authService.activate(request);
    }
}
```

## 29. Auth Service Interface

```java
public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void activate(ActivateAccountRequest request);

    void logout(String refreshToken);
}
```

## 30. Core Registration Implementation

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        String email =
                request.email().trim().toLowerCase();

        if (authUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        Instant now = Instant.now();

        AuthUser user = new AuthUser();

        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.CANDIDATE);
        user.setStatus(
                AccountStatus.PENDING_ACTIVATION
        );
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        authUserRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        AuthUser user =
                authUserRepository
                    .findByEmailIgnoreCase(request.email())
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "Invalid credentials"
                        ));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Account is not active"
            );
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Invalid credentials"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        user.setLastLoginAt(Instant.now());

        return new AuthResponse(
                accessToken,
                null,
                "Bearer",
                900
        );
    }
}
```

The `refresh()` method must be implemented before treating the service as complete; the example above intentionally shows the core MVP registration/login logic.

## 31. Failed Login Protection

The database contains:

```text
failed_login_attempts
locked_until
```

Recommended policy:

```text
5 failed attempts
       |
       v
Temporary lock
       |
       v
15 minutes
```

On successful login:

```text
failed_login_attempts = 0
```

Do not permanently lock a user after a small number of failed attempts.

Also add rate limiting at the gateway/security layer later.

## 32. Logout

JWT access tokens are short-lived.

Logout should revoke refresh tokens:

```text
POST /api/v1/auth/logout
       |
       v
Find refresh token
       |
       v
revoked = true
```

After logout, that refresh token cannot create a new access token.

## 33. Password Change

Later:

```http
POST /api/v1/auth/change-password
```

Flow:

```text
Authenticate
    |
Verify current password
    |
Validate new password
    |
BCrypt encode
    |
Save
    |
Revoke all refresh tokens
```

Never return the password hash.

## 34. Forgot Password

Later:

```http
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

Flow:

```text
Forgot password
       |
Generate random reset token
       |
Store token hash
       |
Send email
       |
User submits token
       |
Set new password
       |
Invalidate token
       |
Revoke refresh tokens
```

Do not reveal whether the email is registered.

## 35. User Service Integration

Auth Service and User Service are separate.

Recommended future registration flow:

```text
Auth Service
    |
    | UserRegistered event
    v
Kafka
    |
    v
User Service
    |
    v
Create candidate profile
```

The Auth Service should not create JPA relationships to User Service entities.

Use UUID references and events/API contracts instead.

## 36. Transactional Outbox

When Kafka events are introduced, do not simply do:

```text
save DB
send Kafka
```

Use:

```text
Transaction
   |
   +--> auth_users
   |
   +--> outbox_events
             |
             v
      Outbox Publisher
             |
             v
           Kafka
```

This prevents the case where the database commits but Kafka publishing fails.

## 37. Error Format

Use the same format across all 366PI services:

```json
{
  "timestamp": "2026-09-08T10:00:00Z",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid credentials",
  "traceId": "abc123"
}
```

Useful auth error codes:

```text
INVALID_CREDENTIALS
ACCOUNT_NOT_ACTIVE
ACCOUNT_LOCKED
ACCOUNT_DISABLED
TOKEN_INVALID
TOKEN_EXPIRED
REFRESH_TOKEN_REVOKED
ACTIVATION_TOKEN_EXPIRED
EMAIL_ALREADY_REGISTERED
```

## 38. Observability

Auth Service must include Actuator.

Useful endpoints:

```text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

Later add:

```text
Micrometer
Prometheus
Grafana
OpenTelemetry
```

Never log:

```text
password
JWT
refresh token
activation token
password reset token
```

## 39. Eureka Registration

Eureka configuration:

```yaml
spring:
  application:
    name: auth-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

Eureka dashboard should eventually show:

```text
AUTH-SERVICE
USER-SERVICE
ADMIN-SERVER
```

## 40. API Gateway Integration Later

Gateway will route:

```text
/api/v1/auth/**     -> AUTH-SERVICE
/api/v1/users/**    -> USER-SERVICE
/api/v1/jobs/**     -> JOB-SERVICE
/api/v1/applications/** -> APPLICATION-SERVICE
/api/v1/interviews/**   -> INTERVIEW-SERVICE
```

The frontend should call only:

```text
API Gateway :8080
```

not individual services.

## 41. Security Rules

Follow these rules:

1. Never store plaintext passwords.
2. Use BCrypt/Argon2 rather than custom hashing.
3. Never put sensitive information into JWT.
4. Keep access tokens short-lived.
5. Rotate refresh tokens.
6. Store refresh-token hashes, not raw refresh tokens.
7. Revoke refresh tokens on logout/password change.
8. Do not allow public users to select ADMIN role.
9. Do not reveal whether an email exists during login/reset flows.
10. Rate-limit authentication endpoints.
11. Never log passwords or tokens.
12. Use HTTPS in production.
13. Store JWT secrets outside Git.
14. Validate all incoming requests.
15. Enforce authorization on the server.
16. Keep Auth Service separate from User Service.

## 42. Testing Checklist

```text
[ ] Registration works
[ ] Duplicate email rejected
[ ] Password is BCrypt hashed
[ ] Password hash never returned
[ ] Candidate role assigned by server
[ ] Account starts PENDING_ACTIVATION
[ ] Activation works
[ ] Activation token expires
[ ] Activation token is single-use
[ ] Login works for ACTIVE account
[ ] Wrong password rejected
[ ] Unknown email returns same generic error
[ ] Failed-login counter works
[ ] Temporary account lock works
[ ] JWT is generated
[ ] JWT validation works
[ ] Invalid JWT rejected
[ ] Expired JWT rejected
[ ] Role is available in SecurityContext
[ ] Refresh token works
[ ] Refresh token rotation works
[ ] Revoked refresh token rejected
[ ] Logout revokes refresh token
[ ] Password change revokes refresh tokens
[ ] Actuator health works
[ ] Eureka registration works
[ ] Spring Boot Admin discovers auth-service
```

## 43. Postman Test Sequence

### Register

```http
POST http://localhost:8083/api/v1/auth/register
Content-Type: application/json
```

```json
{
  "email": "candidate@example.com",
  "password": "StrongPassword123!"
}
```

### Activate

```http
POST http://localhost:8083/api/v1/auth/activate
Content-Type: application/json
```

```json
{
  "token": "<activation-token>",
  "password": "StrongPassword123!"
}
```

### Login

```http
POST http://localhost:8083/api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "candidate@example.com",
  "password": "StrongPassword123!"
}
```

Expected:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### Authenticated request

```http
Authorization: Bearer <access-token>
```

### Refresh

```http
POST http://localhost:8083/api/v1/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<refresh-token>"
}
```

## 44. Git Strategy

```bash
git checkout -b feature/auth-service
```

Recommended commits:

```text
feat: initialize auth service
feat: configure postgres and flyway
feat: add auth user entity
feat: add registration flow
feat: add account activation
feat: add jwt authentication
feat: add refresh token flow
feat: add logout
feat: add security exception handling
test: add auth service tests
```

Merge into:

```text
feature/auth-service
        |
        v
       dev
```

## 45. Local Architecture After Auth Service

```text
                       366PI

                +----------------+
                | Eureka :8761   |
                +-------+--------+
                        |
          +-------------+-------------+
          |             |             |
          v             v             v
   User Service   Auth Service   Admin Server
      :8081          :8083          :8082
        |              |
        v              v
     user_db        auth_db

Later:

                    Gateway :8080
                         |
       +-----------------+------------------+
       |                 |                  |
       v                 v                  v
     Auth              User               Jobs
```

## 46. Definition of Done

Auth Service is ready for the next domain only when:

```text
[ ] It starts independently.
[ ] It connects to auth_db.
[ ] Flyway creates the schema.
[ ] It registers with Eureka.
[ ] Admin Server monitors it.
[ ] Registration works.
[ ] Activation works.
[ ] Passwords are hashed.
[ ] Login works.
[ ] JWT access token works.
[ ] Refresh token flow works.
[ ] Logout/revocation works.
[ ] Invalid credentials are handled safely.
[ ] Role-based authorization works.
[ ] Security-sensitive values are never logged.
[ ] Unit tests pass.
[ ] Integration tests pass.
```

## 47. Recommended Implementation Order

Implement in this order:

```text
1. Create auth-service
2. Configure PostgreSQL
3. Add Flyway
4. Add AuthUser
5. Add registration
6. Add activation
7. Add BCrypt
8. Add JWT
9. Add SecurityConfig
10. Add JWT filter
11. Add login
12. Add refresh-token rotation
13. Add logout
14. Add failed-login protection
15. Add password change/reset
16. Add Eureka
17. Add Actuator
18. Add tests
19. Add Kafka + Outbox
20. Integrate with User Service
```

The important architectural boundary is:

```text
AUTH SERVICE
    |
    +-- Who are you?
    +-- Are you authenticated?
    +-- What role do you have?
    +-- Is your account active?
    +-- Can you obtain/refresh a token?

USER SERVICE
    |
    +-- What is your profile?
    +-- What are your skills?
    +-- What is your education?
    +-- What resume belongs to you?
```
