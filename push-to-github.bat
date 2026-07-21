@echo off
title Push Ice Client to GitHub
cd /d "%~dp0"

REM Push whatever branch is actually checked out. This used to be hardcoded to
REM "main", which quietly pushed nothing whenever the work sat on another branch.
for /f "delims=" %%b in ('git rev-parse --abbrev-ref HEAD') do set BRANCH=%%b

echo Pushing branch "%BRANCH%" to https://github.com/cledusdonald-png/ice-client ...
echo (A GitHub sign-in window may pop up the first time -- complete it.)
echo.
git push -u origin %BRANCH%
echo.

if errorlevel 1 (
  echo Push FAILED. Screenshot the red text above and send it over.
) else (
  echo Done -- "%BRANCH%" is on GitHub.
)
pause
