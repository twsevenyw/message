#!/bin/bash

# Network Access Test Script for Encrypted Messaging Server

echo "🔐 Encrypted Messaging Server - Network Access Info"
echo "=================================================="

# Get local IP address
LOCAL_IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)

echo "📱 Your server is accessible at:"
echo "   http://$LOCAL_IP:5000"
echo ""
echo "🔑 Channel password: securechannel123"
echo ""
echo "📋 Share this information with others on your network:"
echo "   • URL: http://$LOCAL_IP:5000"
echo "   • Password: securechannel123"
echo ""
echo "🌐 Devices that can access:"
echo "   • Other computers on your WiFi"
echo "   • Phones/tablets on the same network"
echo "   • Any device connected to your local network"
echo ""
echo "⚠️  Security Note:"
echo "   • Only devices on your local network can access"
echo "   • Server is NOT exposed to the internet"
echo "   • All messages are encrypted with AES-256"
echo ""
echo "🚀 To start the server:"
echo "   ./start_server.sh"

