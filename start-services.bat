@echo off
title 366PI Core Services Launcher (Eureka, Admin, Auth, User)
echo =======================================================
echo   Starting 366PI Core Microservices
echo   1. Eureka Discovery Server  :8761
echo   2. Spring Boot Admin Server :8082
echo   3. Auth Service             :8083
echo   4. User Service             :8081
echo =======================================================

echo [1/4] Starting Eureka Discovery Server (Port 8761)...
start "Discovery-Server (8761)" cmd /c "cd /d %~dp0discovery-server && ..\mvnw.cmd spring-boot:run"
echo Waiting 12 seconds for Eureka to initialize...
timeout /t 12 /nobreak >nul

echo [2/4] Starting Spring Boot Admin Server (Port 8082)...
start "Admin-Server (8082)" cmd /c "cd /d %~dp0admin-server && ..\mvnw.cmd spring-boot:run"
timeout /t 6 /nobreak >nul

echo [3/4] Starting Auth Service (Port 8083)...
start "Auth-Service (8083)" cmd /c "cd /d %~dp0auth-service && ..\mvnw.cmd spring-boot:run"

echo [4/4] Starting User Service (Port 8081)...
start "User-Service (8081)" cmd /c "cd /d %~dp0user-service && ..\mvnw.cmd spring-boot:run"

echo =======================================================
echo   All 4 core services have been launched!
echo   Eureka Dashboard:       http://localhost:8761
echo   Spring Boot Admin:      http://localhost:8082
echo   Auth Service Swagger:   http://localhost:8083/swagger-ui.html
echo   User Service Swagger:   http://localhost:8081/swagger-ui.html
echo =======================================================
pause
