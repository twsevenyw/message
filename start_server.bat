@echo off
REM Start the Secure Encrypted Messaging Server (Windows)

echo 🔐 Starting Secure Encrypted Messaging Server...

REM Check if virtual environment exists
if not exist "venv" (
    echo ❌ Virtual environment not found. Please run setup.bat first.
    pause
    exit /b 1
)

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Check if dependencies are installed
python -c "import flask" >nul 2>&1
if errorlevel 1 (
    echo 📥 Installing web dependencies...
    pip install -r requirements.txt
)

echo 🚀 Starting web server on http://localhost:5000
echo 📱 Open your browser and go to: http://localhost:5000
echo 🔑 Demo password: securechannel123
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start the web server
python web_server.py
pause

