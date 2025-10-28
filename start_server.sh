#!/bin/bash

# Start the Secure Encrypted Messaging Server
echo "🔐 Starting Secure Encrypted Messaging Server..."

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "❌ Virtual environment not found. Please run setup.sh first."
    exit 1
fi

# Activate virtual environment
source venv/bin/activate

# Check if dependencies are installed
if ! python -c "import flask" 2>/dev/null; then
    echo "📥 Installing web dependencies..."
    pip install -r requirements.txt
fi

echo "🚀 Starting web server on http://localhost:5000"
echo "📱 Network access: http://$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1):5000"
echo "🔑 Demo password: securechannel123"
echo ""
echo "📋 Share with others on your network:"
echo "   URL: http://$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1):5000"
echo "   Password: securechannel123"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start the web server
python web_server.py
