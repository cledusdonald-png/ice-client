@echo off
title Ice Client Launcher
cd /d "%~dp0"

where npm >nul 2>nul
if errorlevel 1 (
  echo.
  echo   Node.js isn't installed yet.
  echo   Get the LTS version from https://nodejs.org , install it,
  echo   then double-click this file again.
  echo.
  pause
  exit /b
)

echo Checking for updates ^(quick after the first time^)...
call npm install

echo Starting Ice Client Launcher...
call npm start
