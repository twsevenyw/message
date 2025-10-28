#!/usr/bin/env python3
"""
Simple Text Cryptography Software
A user-friendly tool for encrypting and decrypting text using AES encryption.
"""

import os
import base64
import getpass
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC


class TextCryptographer:
    """A simple class for encrypting and decrypting text using AES encryption."""
    
    def __init__(self):
        self.fernet = None
    
    def derive_key_from_password(self, password: str, salt: bytes = None) -> bytes:
        """Derive a cryptographic key from a password using PBKDF2."""
        if salt is None:
            salt = os.urandom(16)  # Generate a random salt
        
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=32,
            salt=salt,
            iterations=100000,  # High iteration count for security
        )
        key = base64.urlsafe_b64encode(kdf.derive(password.encode()))
        return key, salt
    
    def encrypt_text(self, text: str, password: str) -> tuple:
        """
        Encrypt text using the provided password.
        Returns (encrypted_text, salt) as base64 encoded strings.
        """
        try:
            key, salt = self.derive_key_from_password(password)
            fernet = Fernet(key)
            
            encrypted_bytes = fernet.encrypt(text.encode())
            encrypted_text = base64.urlsafe_b64encode(encrypted_bytes).decode()
            salt_b64 = base64.urlsafe_b64encode(salt).decode()
            
            return encrypted_text, salt_b64
        except Exception as e:
            raise Exception(f"Encryption failed: {str(e)}")
    
    def decrypt_text(self, encrypted_text: str, password: str, salt_b64: str) -> str:
        """
        Decrypt text using the provided password and salt.
        """
        try:
            # Decode salt and encrypted text
            salt = base64.urlsafe_b64decode(salt_b64.encode())
            encrypted_bytes = base64.urlsafe_b64decode(encrypted_text.encode())
            
            # Derive key using the same salt
            key, _ = self.derive_key_from_password(password, salt)
            fernet = Fernet(key)
            
            decrypted_bytes = fernet.decrypt(encrypted_bytes)
            return decrypted_bytes.decode()
        except Exception as e:
            raise Exception(f"Decryption failed: {str(e)}")


def get_user_input():
    """Get user input for the operation and text."""
    print("\n" + "="*50)
    print("🔐 SIMPLE TEXT CRYPTOGRAPHER 🔐")
    print("="*50)
    print("1. Encrypt text")
    print("2. Decrypt text")
    print("3. Exit")
    
    while True:
        choice = input("\nChoose an option (1-3): ").strip()
        if choice in ['1', '2', '3']:
            break
        print("Please enter 1, 2, or 3.")
    
    if choice == '3':
        print("Goodbye! 👋")
        return None, None, None
    
    # Get text input
    if choice == '1':
        print("\nEnter the text you want to encrypt:")
        text = input("> ")
    else:  # choice == '2'
        print("\nEnter the encrypted text:")
        encrypted_text = input("> ")
        print("\nEnter the salt (base64 encoded):")
        salt = input("> ")
        text = encrypted_text
    
    # Get password
    password = getpass.getpass("\nEnter password: ")
    if not password:
        print("Password cannot be empty!")
        return None, None, None
    
    return choice, text, password, salt if choice == '2' else None


def main():
    """Main function to run the cryptography software."""
    cryptographer = TextCryptographer()
    
    print("Welcome to Simple Text Cryptographer!")
    print("This tool uses AES encryption to secure your text.")
    
    while True:
        try:
            result = get_user_input()
            if result[0] is None:  # User chose exit or invalid input
                break
            
            choice, text, password, salt = result
            
            if choice == '1':  # Encrypt
                if not text.strip():
                    print("Text cannot be empty!")
                    continue
                
                encrypted_text, salt_b64 = cryptographer.encrypt_text(text, password)
                
                print("\n" + "="*50)
                print("✅ ENCRYPTION SUCCESSFUL!")
                print("="*50)
                print(f"Encrypted text: {encrypted_text}")
                print(f"Salt (save this!): {salt_b64}")
                print("\n⚠️  IMPORTANT: Save both the encrypted text and salt!")
                print("   You'll need both to decrypt your text later.")
                
            elif choice == '2':  # Decrypt
                if not text.strip() or not salt.strip():
                    print("Both encrypted text and salt are required!")
                    continue
                
                decrypted_text = cryptographer.decrypt_text(text, password, salt)
                
                print("\n" + "="*50)
                print("✅ DECRYPTION SUCCESSFUL!")
                print("="*50)
                print(f"Decrypted text: {decrypted_text}")
        
        except KeyboardInterrupt:
            print("\n\nOperation cancelled. Goodbye! 👋")
            break
        except Exception as e:
            print(f"\n❌ Error: {str(e)}")
            print("Please check your input and try again.")
        
        # Ask if user wants to continue
        continue_choice = input("\nDo you want to perform another operation? (y/n): ").strip().lower()
        if continue_choice not in ['y', 'yes']:
            print("Goodbye! 👋")
            break


if __name__ == "__main__":
    main()
