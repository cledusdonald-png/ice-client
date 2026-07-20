@echo off
title Push Ice Client to GitHub
cd /d "%~dp0"
echo Pushing to https://github.com/cledusdonald-png/ice-client ...
echo (A GitHub sign-in window may pop up the first time -- complete it.)
echo.
git push -u origin main
echo.
echo Done. If it succeeded, your code is on GitHub.
pause
