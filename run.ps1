# CivicFix PowerShell Launch Script
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\Jeet\.gemini\antigravity\tools\apache-maven-3.9.6\bin;$env:Path"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Starting CivicFix - Civic Issue Management Platform" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

java -version
mvn -version

Write-Host "`nStarting Spring Boot on http://localhost:8080 ..." -ForegroundColor Green
mvn spring-boot:run
