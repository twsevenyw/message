#!/bin/bash

# Internet Deployment Helper Script

echo "🌐 Internet Deployment Helper"
echo "============================="

# Check if we're online
if ! ping -c 1 google.com >/dev/null 2>&1; then
    echo "❌ No internet connection detected"
    exit 1
fi

# Get public IP
echo "🔍 Finding your public IP address..."
PUBLIC_IP=$(curl -s ifconfig.me 2>/dev/null || curl -s ipinfo.io/ip 2>/dev/null)

if [ -z "$PUBLIC_IP" ]; then
    echo "❌ Could not determine public IP address"
    echo "   Please visit: https://whatismyipaddress.com/"
    exit 1
fi

echo "✅ Your public IP address: $PUBLIC_IP"
echo ""

# Check if server is running
if ! pgrep -f "internet_server.py" >/dev/null; then
    echo "🚀 Starting internet server..."
    echo "   This will make your server accessible from the internet!"
    echo ""
    echo "⚠️  SECURITY WARNING:"
    echo "   • Your server will be accessible from anywhere on the internet"
    echo "   • Make sure you have proper firewall/security measures"
    echo "   • Consider changing the default password"
    echo ""
    read -p "Continue? (y/N): " -n 1 -r
    echo ""
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🌐 Starting server on port 8080..."
        echo "📱 Internet URL: http://$PUBLIC_IP:8080"
        echo "🔑 Password: securechannel123"
        echo ""
        echo "📋 Share this information:"
        echo "   URL: http://$PUBLIC_IP:8080"
        echo "   Password: securechannel123"
        echo ""
        echo "⚠️  Make sure to configure port forwarding on your router!"
        echo "   External Port: 8080"
        echo "   Internal Port: 8080"
        echo "   Internal IP: $(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)"
        echo ""
        echo "Press Ctrl+C to stop the server"
        echo ""
        
        python internet_server.py
    else
        echo "❌ Deployment cancelled"
    fi
else
    echo "✅ Internet server is already running"
    echo "📱 Internet URL: http://$PUBLIC_IP:8080"
    echo "🔑 Password: securechannel123"
fi
