@echo off
title Ice Client
cd /d "%~dp0"

if not exist "node_modules\electron\dist\electron.exe" (
  echo.
  echo   This folder looks incomplete.
  echo   Make sure you extracted the ENTIRE zip ^(not run it from inside the zip^),
  echo   then double-click run.bat again.
  echo.
  pause
  exit /b
)

"node_modules\electron\dist\electron.exe" .
