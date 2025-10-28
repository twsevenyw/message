#!/bin/bash

# Complete Vercel Deployment Script

echo "🚀 Complete Vercel Deployment Script"
echo "===================================="

# Check if we're in the right directory
if [ ! -f "vercel.json" ]; then
    echo "❌ Please run this script from the project root directory"
    exit 1
fi

echo "✅ Project files found"
echo ""

# Check if user is logged in to Vercel
echo "🔐 Checking Vercel authentication..."
if ! npx vercel whoami &> /dev/null; then
    echo "📝 Please login to Vercel..."
    echo "   This will open a browser window for authentication"
    echo ""
    npx vercel login
    echo ""
fi

echo "🌐 Deploying to Vercel..."
echo ""

# Deploy to Vercel
npx vercel --yes

echo ""
echo "✅ Deployment initiated!"
echo ""
echo "🔧 IMPORTANT: Set Environment Variables"
echo "1. Go to your Vercel dashboard: https://vercel.com/dashboard"
echo "2. Click on your project"
echo "3. Go to Settings → Environment Variables"
echo "4. Add these variables:"
echo "   - FLASK_SECRET_KEY: your-super-secret-key"
echo "   - CHANNEL_PASSWORD: your-secure-password"
echo ""
echo "5. Redeploy with: npx vercel --prod"
echo ""
echo "🌐 Your app will be live at: https://your-project-name.vercel.app"
echo ""
echo "📱 Share with others:"
echo "   URL: https://your-project-name.vercel.app"
echo "   Password: [whatever you set in CHANNEL_PASSWORD]"
