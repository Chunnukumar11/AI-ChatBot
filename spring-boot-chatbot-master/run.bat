@echo off
REM ========================================
REM Web-Based Chatbot - Project Runner
REM ========================================
REM This batch script builds and runs the chatbot application

cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║       Web-Based Chatbot - Spring Boot Application             ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

REM Get the project directory
set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%"

echo [INFO] Project Directory: %PROJECT_DIR%
echo.

REM Step 1: Clean and Build
echo [BUILDING] Compiling project with Maven...
echo ----------------------------------------
call mvn clean compile package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Build failed! Please check your Maven installation.
    pause
    exit /b 1
)
echo [SUCCESS] Build completed successfully!
echo.

REM Step 2: Run the application
echo [RUNNING] Starting application on http://localhost:8080
echo ----------------------------------------
echo.
echo [INFO] Application started! Open your browser and navigate to:
echo        http://localhost:8080
echo.
echo [TEST] Test the chatbot with these hardcoded responses:
echo        * 'Hello' or 'Hi' - Greeting responses
echo        * 'How are you?' - How are you responses
echo        * 'What is DevOps?' - DevOps explanation
echo        * 'What is Maven?' - Maven explanation
echo        * 'What is Spring Boot?' - Spring Boot explanation
echo        * 'Help' - Show available commands
echo.
echo [STOP] Press Ctrl+C to stop the application
echo ----------------------------------------
echo.

REM Run the JAR file
java -jar "%PROJECT_DIR%target\chatbot-1.0.0.jar"
pause
