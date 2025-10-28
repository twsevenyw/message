#!/bin/bash

# Vercel Deployment Script for Encrypted Messaging Server

echo "🚀 Vercel Deployment Helper"
echo "=========================="

# Check if Vercel CLI is installed
if ! command -v vercel &> /dev/null; then
    echo "📥 Installing Vercel CLI..."
    npm install -g vercel
fi

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    echo "📁 Initializing Git repository..."
    git init
    git add .
    git commit -m "Initial commit - Encrypted messaging server"
    echo "⚠️  Please push to GitHub/GitLab first:"
    echo "   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git"
    echo "   git push -u origin main"
    echo ""
    read -p "Have you pushed to GitHub? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Please push to GitHub first, then run this script again"
        exit 1
    fi
fi

# Check if user is logged in to Vercel
if ! vercel whoami &> /dev/null; then
    echo "🔐 Please login to Vercel..."
    vercel login
fi

echo "🌐 Deploying to Vercel..."
echo ""

# Deploy to Vercel
vercel

echo ""
echo "✅ Deployment complete!"
echo ""
echo "🔧 Next steps:"
echo "1. Go to your Vercel dashboard"
echo "2. Set environment variables:"
echo "   - FLASK_SECRET_KEY: your-secret-key"
echo "   - CHANNEL_PASSWORD: your-secure-password"
echo "3. Redeploy: vercel --prod"
echo ""
echo "🌐 Your app will be available at: https://your-app-name.vercel.app"
