# Open H2 Console for NerveMind Database
# This script opens the H2 database console with the correct connection settings

# Set the H2 console URL
$H2_CONSOLE_URL = "http://localhost:8080/h2-console"

# Check if the application is running
$javaProcess = Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*NerveMindApplication*"
}

if (-not $javaProcess) {
    Write-Host "Warning: No NerveMind application process found running." -ForegroundColor Yellow
    Write-Host "Make sure to start the application first with: .\gradlew.bat :app:bootRun" -ForegroundColor Yellow
    Write-Host ""
}

# Open the H2 console in the default browser
Write-Host "Opening H2 Console at: $H2_CONSOLE_URL" -ForegroundColor Green
Write-Host ""
Write-Host "Connection Details:" -ForegroundColor Cyan
Write-Host "JDBC URL: jdbc:h2:file:./data/nervemind;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE" -ForegroundColor White
Write-Host "User Name: sa" -ForegroundColor White
Write-Host "Password: (leave blank)" -ForegroundColor White
Write-Host ""
Write-Host "Note: Select 'Generic H2 (Embedded)' or 'Generic H2 (Server)' and paste the JDBC URL above." -ForegroundColor Yellow

Start-Process $H2_CONSOLE_URL