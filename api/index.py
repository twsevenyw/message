from flask import Flask, render_template, request, redirect, url_for, flash, session
import os
import sqlite3
from encryptor import TextCryptographer

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'your-secret-key-change-this')

# In-memory database for Vercel (consider external DB for production)
DATABASE = ':memory:'

def init_db():
    conn = sqlite3.connect(DATABASE)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            encrypted_content TEXT NOT NULL,
            salt TEXT NOT NULL,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            author TEXT DEFAULT 'Anonymous'
        )
    ''')
    conn.commit()
    conn.close()

def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/')
def index():
    if 'channel_authenticated' in session:
        return redirect(url_for('channel'))
    return render_template('index.html')

@app.route('/access_channel', methods=['POST'])
def access_channel():
    channel_password = request.form['password']
    correct_password = os.environ.get('CHANNEL_PASSWORD', 'securechannel123')
    
    if channel_password == correct_password:
        session['channel_authenticated'] = True
        session['channel_password'] = channel_password
        flash('Welcome to the secure channel!', 'success')
        return redirect(url_for('channel'))
    else:
        flash('Invalid password!', 'error')
        return redirect(url_for('index'))

@app.route('/channel')
def channel():
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    conn = get_db_connection()
    messages = conn.execute('SELECT * FROM messages ORDER BY timestamp DESC LIMIT 50').fetchall()
    conn.close()
    return render_template('channel.html', messages=messages)

@app.route('/post_message', methods=['POST'])
def post_message():
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    message_content = request.form['message']
    author = request.form.get('author', 'Anonymous')
    
    if not message_content.strip():
        flash('Message cannot be empty!', 'error')
        return redirect(url_for('channel'))
    
    try:
        cryptographer = TextCryptographer()
        channel_password = session['channel_password']
        encrypted_content, salt = cryptographer.encrypt_text(message_content, channel_password)
        
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
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    conn = get_db_connection()
    message = conn.execute('SELECT * FROM messages WHERE id = ?', (message_id,)).fetchone()
    conn.close()
    
    if not message:
        flash('Message not found!', 'error')
        return redirect(url_for('channel'))
    
    try:
        cryptographer = TextCryptographer()
        channel_password = session['channel_password']
        decrypted_content = cryptographer.decrypt_text(
            message['encrypted_content'], 
            channel_password, 
            message['salt']
        )
        return render_template('decrypt.html', message=message, decrypted_content=decrypted_content)
    except Exception as e:
        flash(f'Error decrypting message: {str(e)}', 'error')
        return redirect(url_for('channel'))

@app.route('/logout')
def logout():
    session.clear()
    flash('Logged out successfully!', 'info')
    return redirect(url_for('index'))

# Initialize database on startup
init_db()

# Vercel expects the app to be available
if __name__ == '__main__':
    app.run(debug=True)