@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.15.6-hotspot"
call gradlew.bat bootRun --console=plain
pause
