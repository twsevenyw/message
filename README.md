# 🔐 Simple Text Cryptographer

A user-friendly cryptography software with both command-line and web-based interfaces for encrypting and decrypting text using AES encryption with password-based key derivation.

## 🌐 Web Server Version

**NEW!** Now includes a web server for encrypted messaging channels where multiple users can share encrypted messages!

## Features

- **AES Encryption**: Uses industry-standard AES encryption for maximum security
- **Password Protection**: Your password is never stored - only used to derive encryption keys
- **PBKDF2 Key Derivation**: Uses PBKDF2 with 100,000 iterations for secure key generation
- **Easy to Use**: Simple command-line interface with clear instructions
- **Web Interface**: Beautiful web-based encrypted messaging channel
- **Multi-User Support**: Multiple users can share encrypted messages in password-protected channels
- **Cross-Platform**: Works on Windows, macOS, and Linux

## Installation

### Quick Setup (Recommended)

**For macOS/Linux:**
```bash
./setup.sh
```

**For Windows:**
```batch
setup.bat
```

### Manual Setup

1. **Clone or download** this repository to your computer
2. **Install Python** (version 3.7 or higher) if you haven't already
3. **Create a virtual environment**:
   ```bash
   python3 -m venv venv
   ```
4. **Activate the virtual environment**:
   - macOS/Linux: `source venv/bin/activate`
   - Windows: `venv\Scripts\activate.bat`
5. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

## Usage

### 🌐 Web Server (Encrypted Messaging Channel)

**Start the web server:**

**macOS/Linux:**
```bash
./start_server.sh
```

**Windows:**
```batch
start_server.bat
```

**Then open your browser and go to:** `http://localhost:5000`

**Demo password:** `securechannel123`

#### How the Web Interface Works:
1. **Access Channel**: Enter the channel password to access the encrypted messaging area
2. **Post Messages**: Write messages that get automatically encrypted with AES-256
3. **View Messages**: See all encrypted messages in the channel
4. **Decrypt Messages**: Click "Decrypt Message" to view the decrypted content
5. **Share Securely**: Anyone with the password can decrypt and read messages

#### Web Server Features:
- **Real-time Messaging**: Post and view encrypted messages instantly
- **Password-Protected Channels**: Only users with the correct password can access messages
- **Message History**: View up to 50 recent encrypted messages
- **Beautiful Interface**: Modern, responsive web design
- **Secure Storage**: All messages stored encrypted in SQLite database
- **Session Management**: Secure login/logout functionality

### 💻 Command Line Interface

**After setup (with virtual environment activated):**
```bash
python encryptor.py
```

**If you need to activate the virtual environment first:**
- macOS/Linux: `source venv/bin/activate`
- Windows: `venv\Scripts\activate.bat`

### How to Use

1. **Start the program** by running `python encryptor.py`
2. **Choose an option**:
   - `1` - Encrypt text
   - `2` - Decrypt text  
   - `3` - Exit

#### Encrypting Text

1. Select option `1`
2. Enter the text you want to encrypt
3. Enter a strong password (it won't be visible as you type)
4. **Save both outputs**:
   - The encrypted text
   - The salt (needed for decryption)

#### Decrypting Text

1. Select option `2`
2. Enter the encrypted text
3. Enter the salt (base64 encoded)
4. Enter the password used for encryption
5. Your original text will be displayed

## Security Features

- **AES-256 Encryption**: Military-grade encryption standard
- **Random Salt**: Each encryption uses a unique random salt
- **PBKDF2**: Password-based key derivation with 100,000 iterations
- **No Password Storage**: Your password is never saved anywhere
- **Secure Random Generation**: Uses cryptographically secure random number generation

## Example Usage

```
🔐 SIMPLE TEXT CRYPTOGRAPHER 🔐
==================================================
1. Encrypt text
2. Decrypt text
3. Exit

Choose an option (1-3): 1

Enter the text you want to encrypt:
> This is my secret message!

Enter password: 
[password is hidden]

==================================================
✅ ENCRYPTION SUCCESSFUL!
==================================================
Encrypted text: gAAAAABh...
Salt (save this!): b'abc123...'

⚠️  IMPORTANT: Save both the encrypted text and salt!
   You'll need both to decrypt your text later.
```

## Important Security Notes

- **Keep your password safe**: If you forget it, your encrypted text cannot be recovered
- **Save the salt**: The salt is required for decryption - store it securely
- **Use strong passwords**: Longer passwords with mixed characters are more secure
- **Don't share encrypted text**: Anyone with the password and salt can decrypt your text

## Troubleshooting

### Common Issues

1. **"Decryption failed" error**: 
   - Check that you entered the correct password
   - Verify the salt is exactly as provided during encryption
   - Ensure the encrypted text is complete and unmodified

2. **"Password cannot be empty" error**:
   - Make sure to enter a password when prompted

3. **Import errors**:
   - Run `pip install -r requirements.txt` to install dependencies

## Technical Details

- **Encryption Algorithm**: AES-256 in CBC mode
- **Key Derivation**: PBKDF2-HMAC-SHA256 with 100,000 iterations
- **Salt Size**: 16 bytes (128 bits)
- **Key Size**: 32 bytes (256 bits)
- **Encoding**: Base64 URL-safe encoding for text output

## License

This software is provided as-is for educational and personal use. Use responsibly and keep your passwords secure!

---

**Happy encrypting! 🔐**
