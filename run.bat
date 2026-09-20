@echo off
echo ===================================================
echo Starting CivicFix - Civic Issue Management Platform
echo ===================================================

set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot
set PATH=%JAVA_HOME%\bin;C:\Users\Jeet\.gemini\antigravity\tools\apache-maven-3.9.6\bin;%PATH%

echo Java Version:
java -version

echo Maven Version:
mvn -version

echo.
echo Starting Spring Boot application on http://localhost:8080 ...
mvn spring-boot:run
pause
