from flask import Flask, request, redirect, url_for, session
import os
from encryptor import TextCryptographer

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'your-secret-key-change-this')

# Simple in-memory storage for Vercel
messages = []

@app.route('/')
def index():
    try:
        if 'channel_authenticated' in session:
            return redirect(url_for('channel'))
        
        # Simple HTML response instead of template
        return '''
        <!DOCTYPE html>
        <html>
        <head>
            <title>🔐 Secure Encrypted Channel</title>
            <style>
                body { font-family: Arial, sans-serif; max-width: 500px; margin: 50px auto; padding: 20px; }
                .container { background: #f5f5f5; padding: 30px; border-radius: 10px; text-align: center; }
                h1 { color: #333; }
                input { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 5px; }
                button { background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; }
                .error { color: red; margin: 10px 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🔐 Secure Encrypted Channel</h1>
                <p>Enter the channel password to access encrypted messages</p>
                <form method="POST" action="/access_channel">
                    <input type="password" name="password" placeholder="Enter channel password" required>
                    <br>
                    <button type="submit">Access Channel</button>
                </form>
                <p><strong>Demo Password:</strong> securechannel123</p>
            </div>
        </body>
        </html>
        '''
    except Exception as e:
        return f"Error loading page: {str(e)}", 500

@app.route('/access_channel', methods=['POST'])
def access_channel():
    try:
        channel_password = request.form['password']
        correct_password = os.environ.get('CHANNEL_PASSWORD', 'securechannel123')
        
        if channel_password == correct_password:
            session['channel_authenticated'] = True
            session['channel_password'] = channel_password
            return redirect(url_for('channel'))
        else:
            return redirect(url_for('index'))
    except Exception as e:
        return f"Error in access_channel: {str(e)}", 500

@app.route('/channel')
def channel():
    try:
        if 'channel_authenticated' not in session:
            return redirect(url_for('index'))
        
        # Simple HTML response for channel
        messages_html = ""
        for msg in messages:
            messages_html += f'''
            <div style="background: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px;">
                <strong>{msg['author']}</strong> - {msg['timestamp']}<br>
                <code>{msg['encrypted_content'][:50]}...</code><br>
                <a href="/decrypt_message/{msg['id']}">Decrypt Message</a>
            </div>
            '''
        
        return f'''
        <!DOCTYPE html>
        <html>
        <head>
            <title>🔐 Secure Channel</title>
            <style>
                body {{ font-family: Arial, sans-serif; max-width: 800px; margin: 20px auto; padding: 20px; }}
                .header {{ background: #007bff; color: white; padding: 20px; border-radius: 10px; margin-bottom: 20px; }}
                .post-section {{ background: #f5f5f5; padding: 20px; border-radius: 10px; margin-bottom: 20px; }}
                .messages {{ background: white; padding: 20px; border-radius: 10px; }}
                input, textarea {{ width: 100%; padding: 10px; margin: 5px 0; border: 1px solid #ddd; border-radius: 5px; }}
                button {{ background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; }}
                .logout {{ background: #dc3545; }}
            </style>
        </head>
        <body>
            <div class="header">
                <h1>🔐 Secure Encrypted Channel</h1>
                <a href="/logout" style="color: white; text-decoration: none; background: #dc3545; padding: 5px 10px; border-radius: 3px;">Logout</a>
            </div>
            
            <div class="post-section">
                <h2>📝 Post New Message</h2>
                <form method="POST" action="/post_message">
                    <input type="text" name="author" placeholder="Your name (optional)">
                    <textarea name="message" placeholder="Type your message here..." required></textarea>
                    <button type="submit">🔒 Encrypt & Post Message</button>
                </form>
            </div>
            
            <div class="messages">
                <h2>💬 Encrypted Messages</h2>
                {messages_html if messages_html else '<p>No messages yet. Be the first to post!</p>'}
            </div>
        </body>
        </html>
        '''
    except Exception as e:
        return f"Error loading channel: {str(e)}", 500

@app.route('/post_message', methods=['POST'])
def post_message():
    try:
        if 'channel_authenticated' not in session:
            return redirect(url_for('index'))
        
        message_content = request.form['message']
        author = request.form.get('author', 'Anonymous')
        
        if not message_content.strip():
            return redirect(url_for('channel'))
        
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
        return redirect(url_for('channel'))
    except Exception as e:
        return f"Error posting message: {str(e)}", 500

@app.route('/decrypt_message/<int:message_id>')
def decrypt_message(message_id):
    try:
        if 'channel_authenticated' not in session:
            return redirect(url_for('index'))
        
        message = None
        for msg in messages:
            if msg['id'] == message_id:
                message = msg
                break
        
        if not message:
            return redirect(url_for('channel'))
        
        cryptographer = TextCryptographer()
        channel_password = session['channel_password']
        decrypted_content = cryptographer.decrypt_text(
            message['encrypted_content'], 
            channel_password, 
            message['salt']
        )
        
        return f'''
        <!DOCTYPE html>
        <html>
        <head>
            <title>🔓 Decrypted Message</title>
            <style>
                body {{ font-family: Arial, sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }}
                .container {{ background: #f5f5f5; padding: 30px; border-radius: 10px; }}
                .message {{ background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }}
                button {{ background: #007bff; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; }}
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🔓 Decrypted Message</h1>
                <div class="message">
                    <strong>{message['author']}</strong> - {message['timestamp']}<br><br>
                    {decrypted_content}
                </div>
                <a href="/channel"><button>← Back to Channel</button></a>
            </div>
        </body>
        </html>
        '''
    except Exception as e:
        return f"Error decrypting message: {str(e)}", 500

@app.route('/logout')
def logout():
    try:
        session.clear()
        return redirect(url_for('index'))
    except Exception as e:
        return f"Error logging out: {str(e)}", 500

# For local development
if __name__ == '__main__':
    app.run(debug=True)