@echo off
title Build Ice Client Installer
cd /d "%~dp0"

net session >nul 2>&1
if errorlevel 1 (
  echo.
  echo   This must be run as ADMINISTRATOR.
  echo   Close this, then RIGHT-CLICK build-installer.bat  ->  "Run as administrator".
  echo.
  pause
  exit /b
)

echo Building the Ice Client installer...
call npm install
call npm run dist

echo.
echo If it worked, your installer is here:
echo    dist\Ice Client Setup 0.1.0.exe
echo.
pause
