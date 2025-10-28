from flask import Flask, render_template, request, redirect, url_for, flash, session
import os
from encryptor import TextCryptographer

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'your-secret-key-change-this')

# Simple in-memory storage for Vercel
messages = []

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
        
        message = {
            'id': len(messages) + 1,
            'encrypted_content': encrypted_content,
            'salt': salt,
            'author': author,
            'timestamp': 'Just now'
        }
        messages.append(message)
        flash('Message posted successfully!', 'success')
    except Exception as e:
        flash(f'Error posting message: {str(e)}', 'error')
    
    return redirect(url_for('channel'))

@app.route('/decrypt_message/<int:message_id>')
def decrypt_message(message_id):
    if 'channel_authenticated' not in session:
        flash('Please authenticate first!', 'error')
        return redirect(url_for('index'))
    
    message = None
    for msg in messages:
        if msg['id'] == message_id:
            message = msg
            break
    
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

# For local development
if __name__ == '__main__':
    app.run(debug=True)