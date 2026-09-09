@echo off
title 366PI Microservices Launcher
echo =======================================================
echo   Starting 366PI Recruitment Platform Microservices
echo =======================================================

echo [1/7] Starting Eureka Discovery Server (Port 8761)...
start "Discovery-Server (8761)" cmd /c "cd /d %~dp0discovery-server && ..\mvnw.cmd spring-boot:run"
echo Waiting 12 seconds for Eureka to initialize...
timeout /t 12 /nobreak >nul

echo [2/7] Starting Spring Boot Admin Server (Port 8082)...
start "Admin-Server (8082)" cmd /c "cd /d %~dp0admin-server && ..\mvnw.cmd spring-boot:run"
timeout /t 6 /nobreak >nul

echo [3/7] Starting Auth Service (Port 8083)...
start "Auth-Service (8083)" cmd /c "cd /d %~dp0auth-service && ..\mvnw.cmd spring-boot:run"

echo [4/7] Starting User Service (Port 8081)...
start "User-Service (8081)" cmd /c "cd /d %~dp0user-service && ..\mvnw.cmd spring-boot:run"

echo [5/7] Starting Job Service (Port 8084)...
start "Job-Service (8084)" cmd /c "cd /d %~dp0job-service && ..\mvnw.cmd spring-boot:run"

echo [6/7] Starting Application Service (Port 8085)...
start "Application-Service (8085)" cmd /c "cd /d %~dp0application-service && ..\mvnw.cmd spring-boot:run"

echo [7/7] Starting Interview Service (Port 8086)...
start "Interview-Service (8086)" cmd /c "cd /d %~dp0interview-service && ..\mvnw.cmd spring-boot:run"

echo =======================================================
echo   All 7 microservices have been launched!
echo   Eureka Dashboard:       http://localhost:8761
echo   Spring Boot Admin:      http://localhost:8082
echo =======================================================
pause
