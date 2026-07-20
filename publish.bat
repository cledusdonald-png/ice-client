@echo off
title Publish Ice Client to GitHub
cd /d "%~dp0"

set "msg="
set /p msg=Describe this update (or just press Enter):
if "%msg%"=="" set "msg=Update"

echo.
echo Pushing to GitHub...
git add -A
git commit -m "%msg%"
git push

echo.
echo Done. If you see "Everything up-to-date" there was nothing new to push.
pause
