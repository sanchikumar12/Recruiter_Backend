@echo off
title 366PI Services Stopper
echo =======================================================
echo   Stopping 366PI Core Microservices (8761, 8082, 8083, 8081)
echo =======================================================

set PORTS=8761 8082 8083 8081

for %%P in (%PORTS%) do (
    echo Checking port %%P...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":%%P\>"') do (
        echo Killing process PID %%a on port %%P...
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo =======================================================
echo   Services stopped.
echo =======================================================
pause
