@echo off
REM Setup script for Simple Text Cryptographer (Windows)

echo 🔐 Setting up Simple Text Cryptographer...

REM Check if Python 3 is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Python 3 is not installed. Please install Python 3.7 or higher.
    pause
    exit /b 1
)

REM Create virtual environment if it doesn't exist
if not exist "venv" (
    echo 📦 Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment and install dependencies
echo 📥 Installing dependencies...
call venv\Scripts\activate.bat
pip install -r requirements.txt

echo.
echo ✅ Setup complete!
echo.
echo To run the cryptography software:
echo   venv\Scripts\activate.bat
echo   python encryptor.py
echo.
echo Or run this setup script again anytime to reinstall dependencies.
pause

