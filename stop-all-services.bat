@echo off
title 366PI Microservices Stopper
echo =======================================================
echo   Stopping 366PI Recruitment Platform Microservices
echo =======================================================

set PORTS=8761 8082 8083 8081 8084 8085 8086

for %%P in (%PORTS%) do (
    echo Checking port %%P...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%P\>"') do (
        echo Killing process PID %%a on port %%P...
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo =======================================================
echo   All 366PI microservices have been stopped.
echo =======================================================
pause
