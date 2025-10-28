#!/usr/bin/env python3
"""
Internet-Accessible Encrypted Messaging Server
A web-based encrypted messaging system accessible over the internet.
"""

import os
import sqlite3
import base64
from datetime import datetime
from flask import Flask, render_template, request, redirect, url_for, flash, session
from werkzeug.security import check_password_hash, generate_password_hash
from encryptor import TextCryptographer

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Change this to a fixed secret key in production

# Database setup
DATABASE = 'messages.db'

def init_db():
    """Initialize the database with required tables."""
    conn = sqlite3.connect(DATABASE)
    cursor = conn.cursor()
    
    # Create messages table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            encrypted_content TEXT NOT NULL,
            salt TEXT NOT NULL,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            author TEXT DEFAULT 'Anonymous'
        )
    ''')
    
    # Create channel passwords table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS channel_passwords (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            channel_name TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    conn.commit()
    conn.close()

def get_db_connection():
    """Get a database connection."""
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/')
def index():
    """Main page - channel access."""
    if 'channel_authenticated' in session:
        return redirect(url_for('channel'))
    return render_template('index.html')

@app.route('/access_channel', methods=['POST'])
def access_channel():
    """Handle channel password verification."""
    channel_password = request.form['password']
    
    if not channel_password:
        flash('Password is required!', 'error')
        return redirect(url_for('index'))
    
    # For demo purposes, we'll use a simple password
    # In production, you'd want to store this in the database
    if channel_password == 'securechannel123':
        session['channel_authenticated'] = True
        session['channel_password'] = channel_password
        flash('Welcome to the secure channel!', 'success')
        return redirect(url_for('channel'))
    else:
        flash('Invalid password!', 'error')
        return redirect(url_for('index'))

@app.route('/channel')
def channel():
    """Main channel page showing encrypted messages."""
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    conn = get_db_connection()
    messages = conn.execute(
        'SELECT * FROM messages ORDER BY timestamp DESC LIMIT 50'
    ).fetchall()
    conn.close()
    
    return render_template('channel.html', messages=messages)

@app.route('/post_message', methods=['POST'])
def post_message():
    """Handle posting new encrypted messages."""
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    message_content = request.form['message']
    author = request.form.get('author', 'Anonymous')
    
    if not message_content.strip():
        flash('Message cannot be empty!', 'error')
        return redirect(url_for('channel'))
    
    try:
        # Encrypt the message using the channel password
        cryptographer = TextCryptographer()
        channel_password = session['channel_password']
        encrypted_content, salt = cryptographer.encrypt_text(message_content, channel_password)
        
        # Store in database
        conn = get_db_connection()
        conn.execute(
            'INSERT INTO messages (encrypted_content, salt, author) VALUES (?, ?, ?)',
            (encrypted_content, salt, author)
        )
        conn.commit()
        conn.close()
        
        flash('Message posted successfully!', 'success')
        
    except Exception as e:
        flash(f'Error posting message: {str(e)}', 'error')
    
    return redirect(url_for('channel'))

@app.route('/decrypt_message/<int:message_id>')
def decrypt_message(message_id):
    """Decrypt and display a specific message."""
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    conn = get_db_connection()
    message = conn.execute(
        'SELECT * FROM messages WHERE id = ?', (message_id,)
    ).fetchone()
    conn.close()
    
    if not message:
        flash('Message not found!', 'error')
        return redirect(url_for('channel'))
    
    try:
        # Decrypt the message
        cryptographer = TextCryptographer()
        channel_password = session['channel_password']
        decrypted_content = cryptographer.decrypt_text(
            message['encrypted_content'], 
            channel_password, 
            message['salt']
        )
        
        return render_template('decrypt.html', 
                             message=message, 
                             decrypted_content=decrypted_content)
        
    except Exception as e:
        flash(f'Error decrypting message: {str(e)}', 'error')
        return redirect(url_for('channel'))

@app.route('/logout')
def logout():
    """Logout and clear session."""
    session.clear()
    flash('Logged out successfully!', 'info')
    return redirect(url_for('index'))

if __name__ == '__main__':
    init_db()
    # For internet access, use host='0.0.0.0' and a higher port
    app.run(debug=False, host='0.0.0.0', port=8080)
