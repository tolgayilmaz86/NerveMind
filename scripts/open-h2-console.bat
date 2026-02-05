@echo off
REM Open H2 Console for NerveMind Database
REM This batch file opens the H2 database console

echo Opening H2 Console for NerveMind Database...
echo.
echo Connection Details:
echo JDBC URL: jdbc:h2:file:./data/nervemind;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
echo User Name: sa
echo Password: (leave blank)
echo.
echo Note: Select 'Generic H2 (Embedded)' or 'Generic H2 (Server)' and paste the JDBC URL above.
echo.

REM Check if Java application is running (basic check)
tasklist /FI "IMAGENAME eq java.exe" /NH | findstr /C:"java.exe" >nul
if errorlevel 1 (
    echo Warning: No Java processes found. Make sure the NerveMind application is running.
    echo Start it with: .\gradlew.bat :app:bootRun
    echo.
)

REM Open the H2 console in default browser
start http://localhost:8080/h2-console

echo H2 Console opened in browser.
pause